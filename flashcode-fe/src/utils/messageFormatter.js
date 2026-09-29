// 格式化消息文本
export const formatMessageText = (text) => {
  if (!text) return '';

  const escapeHtml = (str) => {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  };

  // 先处理函数调用格式（在代码块处理之前）
  let formatted = text.replace(/<function=([^>]+)>/g, '<div class="function-call"><strong>函数调用:</strong> $1</div>');
  formatted = formatted.replace(/<\/function>/g, '</div>');
  formatted = formatted.replace(/<parameter=([^>]+)>/g, '<div class="function-param"><strong>参数:</strong> $1</div>');
  formatted = formatted.replace(/<\/parameter>/g, '</div>');
  formatted = formatted.replace(/<tool_call>/g, '<div class="tool-call">');
  formatted = formatted.replace(/<\/tool_call>/g, '</div>');

  // 处理代码块
  const codeBlocks = [];
  let codeBlockIndex = 0;
  formatted = formatted.replace(/```[\s\S]*?```/g, (match) => {
    const placeholder = `__CODE_BLOCK_${codeBlockIndex}__`;
    // 解析 ```lang\ncode``` 形式，保留语言信息
    const m = match.match(/^```([^\n\r`]*)[\r\n]+([\s\S]*?)```$/);
    const lang = (m?.[1] || '').trim();
    const codeContent = (m?.[2] ?? '').trimEnd();
    // 如果代码块太长（超过5000字符），截断并添加提示
    if (codeContent.length > 5000) {
      codeBlocks[codeBlockIndex] = {
        lang,
        code: codeContent.substring(0, 5000) + '\n\n... (代码过长，已截断)',
      };
    } else {
      codeBlocks[codeBlockIndex] = { lang, code: codeContent };
    }
    codeBlockIndex++;
    return placeholder;
  });

  // 处理行内代码
  const inlineCodes = [];
  let inlineCodeIndex = 0;
  formatted = formatted.replace(/`([^`\n]+)`/g, (match, content) => {
    const placeholder = `__INLINE_CODE_${inlineCodeIndex}__`;
    inlineCodes[inlineCodeIndex] = content;
    inlineCodeIndex++;
    return placeholder;
  });

  // 转义 HTML
  formatted = escapeHtml(formatted);

  // 处理标题
  formatted = formatted
    .replace(/^### (.*)$/gim, '<h3>$1</h3>')
    .replace(/^## (.*)$/gim, '<h2>$1</h2>')
    .replace(/^# (.*)$/gim, '<h1>$1</h1>');

  // 处理列表
  const lines = formatted.split('\n');
  const processedLines = [];
  let inList = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const bulletMatch = line.match(/^[-*○]\s+(.*)$/);
    const numberMatch = line.match(/^\d+\.\s+(.*)$/);

    if (bulletMatch || numberMatch) {
      if (!inList) {
        processedLines.push('<ul>');
        inList = true;
      }
      const content = bulletMatch ? bulletMatch[1] : numberMatch[1];
      processedLines.push(`<li>${content}</li>`);
    } else {
      if (inList) {
        processedLines.push('</ul>');
        inList = false;
      }
      processedLines.push(line);
    }
  }

  if (inList) {
    processedLines.push('</ul>');
  }

  formatted = processedLines.join('\n');

  // 处理粗体和斜体
  formatted = formatted
    .replace(/\*\*\*(.*?)\*\*\*/g, '<strong><em>$1</em></strong>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/([^*]|^)\*([^*\n]+?)\*([^*]|$)/g, '$1<em>$2</em>$3');

  // 处理链接 [text](url)
  formatted = formatted.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="message-link">$1</a>');

  // 恢复行内代码
  inlineCodes.forEach((code, index) => {
    formatted = formatted.replace(
      `__INLINE_CODE_${index}__`,
      `<code>${escapeHtml(code)}</code>`
    );
  });

  // 先把非代码块的换行转成 <br>（此时代码块仍是占位符，避免污染代码块内容）
  formatted = formatted.replace(/\n/g, '<br>');

  // 恢复代码块（代码内容必须保持转义，避免 ```html 被当成真实标签渲染）
  codeBlocks.forEach((block, index) => {
    const langClass = block?.lang ? ` language-${escapeHtml(block.lang)}` : '';
    formatted = formatted.replace(
      `__CODE_BLOCK_${index}__`,
      `<pre class="message-code-block"><code class="${langClass.trim()}">${escapeHtml(block?.code ?? '')}</code></pre>`
    );
  });

  return formatted;
};
