/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.cli.auth;

import com.iboot.cli.CliException;
import com.iboot.cli.ExitCode;
import com.iboot.cli.config.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    @TempDir
    Path tempDir;

    private final Context context = new Context("dev", "https://localhost:5173",
            "https://localhost:5173", "iboot-cli", "http://127.0.0.1:8765/callback");

    private final AtomicInteger refreshCount = new AtomicInteger();

    private CredentialStore store() {
        return new CredentialStore(tempDir.resolve("credentials.json"),
                tempDir.resolve("credentials.lock"));
    }

    private TokenService service(CredentialStore store) {
        OidcClient fake = new OidcClient() {
            @Override
            public OidcClient.Discovery discover(Context ignored) {
                return new OidcClient.Discovery("issuer", "auth", "token", null);
            }

            @Override
            public TokenSet refresh(OidcClient.Discovery discovery, String refreshToken,
                                    String clientId) {
                int count = refreshCount.incrementAndGet();
                return new TokenSet("new-" + count, "new-refresh", null,
                        System.currentTimeMillis() + 60_000, "Bearer");
            }
        };
        return new TokenService(store, fake);
    }

    private TokenSet stale() {
        return new TokenSet("old", "old-refresh", null,
                System.currentTimeMillis() - 1_000, "Bearer");
    }

    @Test
    void refreshesAndPersistsNewTokens() {
        CredentialStore store = store();
        store.save("dev", stale());

        TokenSet refreshed = service(store).refresh(context, stale());

        assertThat(refreshed.accessToken()).isEqualTo("new-1");
        assertThat(store.load("dev").accessToken()).isEqualTo("new-1");
        assertThat(refreshCount).hasValue(1);
    }

    @Test
    void usesLatestTokenWhenAnotherProcessAlreadyRefreshed() {
        CredentialStore store = store();
        store.save("dev", stale());
        store.save("dev", new TokenSet("other", "other-refresh", null,
                System.currentTimeMillis() + 60_000, "Bearer"));

        TokenSet refreshed = service(store).refresh(context, stale());

        assertThat(refreshed.accessToken()).isEqualTo("other");
        assertThat(refreshCount).hasValue(0);
    }

    @Test
    void rejectsRefreshWithoutRefreshToken() {
        CredentialStore store = store();
        TokenSet noRefresh = new TokenSet("old", null, null,
                System.currentTimeMillis() - 1_000, "Bearer");
        store.save("dev", noRefresh);

        assertThatThrownBy(() -> service(store).refresh(context, noRefresh))
                .isInstanceOfSatisfying(CliException.class,
                        error -> assertThat(error.exitCode()).isEqualTo(ExitCode.AUTH));
    }

    @Test
    void concurrentRefreshesCallProviderOnce() throws Exception {
        CredentialStore store = store();
        store.save("dev", stale());
        TokenService service = service(store);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TokenSet> first = executor.submit(() -> {
                start.await();
                return service.refresh(context, stale());
            });
            Future<TokenSet> second = executor.submit(() -> {
                start.await();
                return service.refresh(context, stale());
            });
            start.countDown();

            assertThat(first.get(5, TimeUnit.SECONDS).accessToken()).isEqualTo("new-1");
            assertThat(second.get(5, TimeUnit.SECONDS).accessToken()).isEqualTo("new-1");
            assertThat(refreshCount).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
