/**
 * 解码 JWT token
 * @param {string} token - JWT token 字符串
 * @returns {Object|null} 解码后的 payload 对象，解码失败返回 null
 */
export function decodeJWT(token) {
  if (!token || typeof token !== 'string') {
    return null;
  }

  try {
    // JWT 格式：header.payload.signature
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    // 解码 payload（第二部分）
    const payload = parts[1];

    // base64url 解码
    // base64url 使用 - 和 _ 替代 + 和 /，不需要填充 =
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');

    // 添加填充
    while (base64.length % 4) {
      base64 += '=';
    }

    // 解码 base64，正确处理 UTF-8 编码
    // atob() 返回的是 Latin-1 字符串，需要转换为 UTF-8
    const binaryString = atob(base64);

    // 将 Latin-1 字符串转换为 UTF-8 字节数组
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      // charCodeAt 返回的是 0-255 的 Latin-1 字符码，直接作为字节使用
      bytes[i] = binaryString.charCodeAt(i);
    }

    // 使用 TextDecoder 解码 UTF-8 字节数组
    // 注意：TextDecoder 默认就是 UTF-8，但显式指定更安全
    const decoded = new TextDecoder('utf-8', { fatal: false }).decode(bytes);

    // 解析 JSON
    const result = JSON.parse(decoded);

    // 调试：如果 username 包含乱码，输出原始字节
    if (result.username && /[^\x00-\x7F]/.test(result.username) === false && result.username.includes('æ')) {
      console.warn('JWT 解码可能有问题，username:', result.username);
      console.warn('原始 payload:', payload);
      console.warn('解码后的字符串:', decoded);
    }

    return result;
  } catch (error) {
    console.error('JWT 解码失败:', error);
    return null;
  }
}
