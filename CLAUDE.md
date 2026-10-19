# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## 5. JavaDoc 注释规范

适用于新增或修改的 Java 代码；批量重写存量注释需单独立项。

- 语言：说明文字用中文，`@param` / `@return` / `@throws` 等标签名用英文。
- 类/接口/枚举/注解：必须写类级 Javadoc。第一句是摘要（以句号结尾），
  空一行后用 `<p>` 说明职责、分层位置、关键协作对象、事务/线程/缓存约束。
- public/protected 方法：必须写 Javadoc。说明“做什么、前置条件、副作用、异常”，
  不复述方法名；参数、返回值、异常分别用 `@param` / `@return` / `@throws`。
- 字段：DTO 字段、枚举常量、非显而易见的私有字段必须写；局部变量不写。
- 生成成员（getter/setter/equals/hashCode/toString、delombok 构造器）不写 Javadoc。
- 格式：`{@code}` 包类型/字面量/null，`{@link}` 关联类型，分段用 `<p>`，
  列表用 `<ul>/<li>`，示例用 `<pre>{@code ...}</pre>`。
- 禁止：同义反复（`@param id 用户 ID`）、变更历史、个人署名、未转义的 `<`/`>`。
- 类级标签：`@author iBoot Team`、`@since 1.0.0`；只在确有语义时加 `@see`。

详细模板、深度预算与正反例见 `doc/19-JavaDoc 规范.md`。

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
