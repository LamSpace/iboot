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

/**
 * 资源服务器鉴权：JWT 校验、共享会话读取与当前用户访问。
 *
 * <p>本包是「资源服务器契约」的唯一实现。各服务只声明白名单与配置，
 * 不再各自维护过滤器；会话的唯一事实来源仍是共享 Redis。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
package com.iboot.kernel.security;
