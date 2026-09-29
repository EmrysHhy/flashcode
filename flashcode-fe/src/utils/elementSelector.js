/**
 * 创建元素选择处理器
 * @param {Document} doc - 目标文档对象
 * @param {Function} onSelect - 当元素被选中时的回调函数，接收 selector 参数
 * @returns {Object} 包含 init、destroy、removeHighlights 方法的对象
 */
export const createElementSelectionHandler = (doc, onSelect) => {
  let hoveredElement = null;
  let selectedElement = null;
  let highlightStyle = null;

  const highlightElement = (element) => {
    if (!element) return;

    if (highlightStyle) {
      highlightStyle.remove();
    }

    highlightStyle = doc.createElement('style');
    highlightStyle.textContent = `
      .__element-selector-hover {
        outline: 2px dashed #007bff !important;
        outline-offset: 2px !important;
        cursor: pointer !important;
      }
      .__element-selector-selected {
        outline: 3px solid #28a745 !important;
        outline-offset: 2px !important;
      }
    `;
    doc.head.appendChild(highlightStyle);
    element.classList.add('__element-selector-hover');
  };

  const removeHighlights = () => {
    if (highlightStyle) {
      highlightStyle.remove();
      highlightStyle = null;
    }
    const hoverElements = doc.querySelectorAll('.__element-selector-hover, .__element-selector-selected');
    hoverElements.forEach(el => {
      el.classList.remove('__element-selector-hover', '__element-selector-selected');
    });
  };

  const clearSelection = () => {
    // 清除选中状态，允许重新选择
    if (selectedElement) {
      selectedElement.classList.remove('__element-selector-selected');
      selectedElement = null;
    }
    if (hoveredElement) {
      hoveredElement.classList.remove('__element-selector-hover');
      hoveredElement = null;
    }
    removeHighlights();
  };

  const getElementSelector = (element) => {
    if (!element || element.nodeType !== 1) {
      return '';
    }

    const path = [];
    let current = element;

    while (current && current.nodeType === 1) {
      let selector = current.tagName.toLowerCase();

      if (current.id && !current.id.startsWith('__element-selector')) {
        selector = '#' + current.id;
        path.unshift(selector);
        break;
      }

      if (current.className && typeof current.className === 'string') {
        const classes = current.className.trim().split(/\s+/)
          .filter(cls => cls && !cls.startsWith('__element-selector'));
        if (classes.length > 0) {
          selector += '.' + classes.join('.');
        }
      }

      if (current.parentNode) {
        const siblings = Array.from(current.parentNode.children);
        const index = siblings.indexOf(current) + 1;
        const sameTagSiblings = siblings.filter(s => s.tagName === current.tagName);

        if (sameTagSiblings.length > 1) {
          selector += `:nth-child(${index})`;
        }
      }

      path.unshift(selector);

      if (current === doc.body) {
        break;
      }

      current = current.parentElement;
    }

    return path.join(' > ');
  };

  const handleMouseOver = (e) => {
    e.stopPropagation();
    
    // 检查选中的元素是否仍然存在于DOM中，如果不存在则清除选中状态
    if (selectedElement) {
      // 检查元素是否还在DOM中
      if (!doc.contains(selectedElement) || !selectedElement.isConnected) {
        selectedElement = null;
      } else {
        return; // 如果元素还在，不允许重新选择
      }
    }

    if (hoveredElement) {
      hoveredElement.classList.remove('__element-selector-hover');
    }
    hoveredElement = e.target;
    highlightElement(hoveredElement);
  };

  const handleMouseOut = (e) => {
    if (selectedElement) return;
    if (hoveredElement) {
      hoveredElement.classList.remove('__element-selector-hover');
      hoveredElement = null;
    }
  };

  const handleClick = (e) => {
    e.preventDefault();
    e.stopPropagation();
    e.stopImmediatePropagation();

    if (hoveredElement) {
      if (selectedElement) {
        selectedElement.classList.remove('__element-selector-selected');
      }

      selectedElement = hoveredElement;
      selectedElement.classList.remove('__element-selector-hover');
      selectedElement.classList.add('__element-selector-selected');

      const selector = getElementSelector(selectedElement);
      if (selector && onSelect) {
        onSelect(selector);
      }
    }
  };

  return {
    init() {
      doc.addEventListener('mouseover', handleMouseOver, true);
      doc.addEventListener('mouseout', handleMouseOut, true);
      doc.addEventListener('click', handleClick, true);
      doc.body.style.cursor = 'crosshair';
    },
    destroy() {
      doc.removeEventListener('mouseover', handleMouseOver, true);
      doc.removeEventListener('mouseout', handleMouseOut, true);
      doc.removeEventListener('click', handleClick, true);
      doc.body.style.cursor = '';
      removeHighlights();
      hoveredElement = null;
      selectedElement = null;
    },
    removeHighlights,
    clearSelection,
  };
};
