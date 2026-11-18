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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialStoreTest {

    @TempDir
    Path tempDir;

    private CredentialStore store() {
        return new CredentialStore(tempDir.resolve("credentials.json"),
                tempDir.resolve("credentials.lock"));
    }

    private TokenSet tokens(String access) {
        return new TokenSet(access, "refresh-" + access, "id-" + access,
                System.currentTimeMillis() + 60_000, "Bearer");
    }

    @Test
    void savesAndLoadsTokensWithSecretPermissions() throws Exception {
        CredentialStore store = store();

        store.save("dev", tokens("a"));

        assertThat(store.load("dev").accessToken()).isEqualTo("a");
        assertThat(store.load("prod")).isNull();
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                tempDir.resolve("credentials.json"));
        assertThat(permissions).containsExactlyInAnyOrder(
                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    }

    @Test
    void keepsContextsIsolated() {
        CredentialStore store = store();
        store.save("dev", tokens("dev-token"));
        store.save("prod", tokens("prod-token"));

        assertThat(store.load("dev").accessToken()).isEqualTo("dev-token");
        assertThat(store.load("prod").accessToken()).isEqualTo("prod-token");
    }

    @Test
    void deleteRemovesOnlyRequestedContext() {
        CredentialStore store = store();
        store.save("dev", tokens("dev-token"));
        store.save("prod", tokens("prod-token"));

        store.delete("dev");

        assertThat(store.load("dev")).isNull();
        assertThat(store.load("prod").accessToken()).isEqualTo("prod-token");
    }

    @Test
    void replacesFileWithoutLeavingTemporaryFiles() throws Exception {
        CredentialStore store = store();
        store.save("dev", tokens("first"));
        store.save("dev", tokens("second"));

        assertThat(store.load("dev").accessToken()).isEqualTo("second");
        try (var files = Files.list(tempDir)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .noneMatch(name -> name.endsWith(".tmp"));
        }
    }

    @Test
    void nestedSaveInsideLockIsReentrant() {
        CredentialStore store = store();

        store.withLock(() -> {
            store.save("dev", tokens("nested"));
            return null;
        });

        assertThat(store.load("dev").accessToken()).isEqualTo("nested");
    }

    @Test
    void serializesConcurrentWritersInSameProcess() throws Exception {
        CredentialStore store = store();
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                start.await();
                store.withLock(() -> {
                    int current = counter.get();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    counter.set(current + 1);
                    return null;
                });
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(counter).hasValue(4);
    }
}
