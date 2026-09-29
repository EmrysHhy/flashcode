/**
 * 根据指定的唯一键对数组进行去重
 * @param {Array} array - 要去重的数组
 * @param {string} key - 用于去重的唯一键字段名，默认为 'id'
 * @returns {Array} 去重后的数组
 */
export function uniqueBy(array, key = 'id') {
  const seen = new Map();
  return array.filter(item => {
    const keyValue = item[key];
    if (keyValue === undefined || keyValue === null) {
      return true; // 保留没有该字段的项
    }
    if (seen.has(keyValue)) {
      return false; // 已存在，过滤掉
    }
    seen.set(keyValue, true);
    return true;
  });
}
