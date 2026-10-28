import MarkdownIt from "markdown-it";
import DOMPurify from "dompurify";

/**
 * 模型输出的 Markdown 渲染工具。
 *
 * 关闭原始 HTML，再经 DOMPurify 清洗，避免模型输出注入脚本；
 * 链接统一加 noopener，断行按 GitHub 风格处理。
 */

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
});

DOMPurify.addHook("afterSanitizeAttributes", (node) => {
  if (node.tagName === "A") {
    node.setAttribute("target", "_blank");
    node.setAttribute("rel", "noopener noreferrer");
  }
});

/**
 * 把 Markdown 文本渲染为可安全插入页面的 HTML。
 *
 * @param text Markdown 文本
 * @returns 清洗后的 HTML
 */
export function renderMarkdown(text: string): string {
  return DOMPurify.sanitize(markdown.render(text ?? ""));
}
