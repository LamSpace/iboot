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
 * 统一响应信封、业务异常、全局异常处理与国际化消息工具。
 *
 * <p>所有对外提供 HTTP 接口的服务共用这一层，保证响应结构与异常语义一致，
 * 使网关的降级响应与业务响应可以被调用方区分。
 *
 * @author iBoot Team
 * @since 1.0.0
 */
package com.iboot.kernel.web;
