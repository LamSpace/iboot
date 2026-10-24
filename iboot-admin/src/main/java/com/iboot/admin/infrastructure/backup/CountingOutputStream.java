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
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.iboot.admin.infrastructure.backup;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 统计写入字节数的输出流。
 *
 * <p>放在加密流之前，用于得到逻辑备份明文的大小；计数不包含文件头与认证标签。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
public class CountingOutputStream extends FilterOutputStream {

    /**
     * 已写入的字节数。
     */
    private long count;

    /**
     * 创建计数输出流。
     *
     * @param out 下游输出流
     */
    public CountingOutputStream(final OutputStream out) {
        super(out);
    }

    @Override
    public void write(final int b) throws IOException {
        out.write(b);
        count++;
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        out.write(b, off, len);
        count += len;
    }

    /**
     * 获取已写入的字节数。
     *
     * @return 字节数
     */
    public long getCount() {
        return count;
    }
}
