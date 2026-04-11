/**
 * 可视化编辑模块
 * 负责处理 iframe 通信、元素选择、悬浮效果等功能
 */

// 选中的元素信息
export interface SelectedElement {
  id: string
  tagName: string
  className: string
  text: string
  xpath: string
  cssSelector: string
  innerHTML: string
  outerHTML: string
  attributes: Record<string, string>
}

// 消息类型
const MESSAGE_TYPE = {
  ENTER_EDIT_MODE: 'VISUAL_EDITOR_ENTER_EDIT_MODE',
  EXIT_EDIT_MODE: 'VISUAL_EDITOR_EXIT_EDIT_MODE',
  ELEMENT_HOVER: 'VISUAL_EDITOR_ELEMENT_HOVER',
  ELEMENT_CLICK: 'VISUAL_EDITOR_ELEMENT_CLICK',
  ELEMENT_SELECTED: 'VISUAL_EDITOR_ELEMENT_SELECTED',
  REMOVE_SELECTED_ELEMENT: 'VISUAL_EDITOR_REMOVE_SELECTED_ELEMENT',
  SCRIPT_READY: 'VISUAL_EDITOR_SCRIPT_READY',
} as const

// 注入到 iframe 中的脚本
const getInjectScript = () => `
(function() {
  // 避免重复注入
  if (window.__visualEditorInjected) {
    console.log('Visual Editor: Already injected, skipping');
    return;
  }
  window.__visualEditorInjected = true;
  console.log('Visual Editor: Script injected successfully');

  let isEditMode = false;
  let currentHoveredElement = null;
  let selectedElements = [];

  // 创建样式
  const style = document.createElement('style');
  style.id = 'visual-editor-styles';
  style.textContent = \`
    .visual-editor-hover {
      outline: 2px dashed #0ea5e9 !important;
      outline-offset: 2px !important;
      cursor: crosshair !important;
      background-color: rgba(14, 165, 233, 0.05) !important;
    }
    .visual-editor-selected {
      outline: 3px solid #f59e0b !important;
      outline-offset: 2px !important;
      background-color: rgba(245, 158, 11, 0.1) !important;
    }
    .visual-editor-selected.visual-editor-hover {
      outline: 3px solid #f59e0b !important;
    }
  \`;
  document.head.appendChild(style);
  console.log('Visual Editor: Styles added');

  // 获取元素的 XPath
  function getXPath(element) {
    if (element.id) {
      return '//*[@id="' + element.id + '"]';
    }

    const paths = [];
    let current = element;

    while (current && current.nodeType === Node.ELEMENT_NODE) {
      let index = 0;
      let sibling = current.previousSibling;

      while (sibling) {
        if (sibling.nodeType === Node.ELEMENT_NODE && sibling.tagName === current.tagName) {
          index++;
        }
        sibling = sibling.previousSibling;
      }

      const tagName = current.tagName.toLowerCase();
      const pathIndex = index > 0 ? '[' + (index + 1) + ']' : '';
      paths.unshift(tagName + pathIndex);

      current = current.parentNode;
    }

    return '/' + paths.join('/');
  }

  // 获取元素的 CSS 选择器
  function getCssSelector(element) {
    if (element.id) {
      return '#' + CSS.escape(element.id);
    }

    const parts = [];
    let current = element;

    while (current && current.nodeType === Node.ELEMENT_NODE && current !== document.body) {
      let selector = current.tagName.toLowerCase();

      if (current.id) {
        selector = '#' + CSS.escape(current.id);
        parts.unshift(selector);
        break;
      }

      if (current.className && typeof current.className === 'string') {
        const classes = current.className.trim().split(/\\s+/).filter(c => c && !c.startsWith('visual-editor'));
        if (classes.length > 0) {
          selector += '.' + classes.map(c => CSS.escape(c)).join('.');
        }
      }

      // 添加 nth-child 如果需要
      let index = 1;
      let sibling = current.previousElementSibling;
      while (sibling) {
        if (sibling.tagName === current.tagName) {
          index++;
        }
        sibling = sibling.previousElementSibling;
      }

      if (index > 1 || current.nextElementSibling?.tagName === current.tagName) {
        selector += ':nth-child(' + index + ')';
      }

      parts.unshift(selector);
      current = current.parentNode;
    }

    return parts.join(' > ');
  }

  // 获取元素信息
  function getElementInfo(element) {
    const attributes = {};
    for (const attr of element.attributes || []) {
      attributes[attr.name] = attr.value;
    }

    return {
      id: element.id || '',
      tagName: element.tagName.toLowerCase(),
      className: (element.className && typeof element.className === 'string')
        ? element.className.replace(/visual-editor-\\w+/g, '').trim()
        : '',
      text: element.textContent?.trim().slice(0, 100) || '',
      xpath: getXPath(element),
      cssSelector: getCssSelector(element),
      innerHTML: element.innerHTML.slice(0, 500),
      outerHTML: element.outerHTML.slice(0, 500),
      attributes
    };
  }

  // 发送消息到父窗口
  function sendToParent(type, data) {
    if (window.parent !== window) {
      window.parent.postMessage({ type, data }, '*');
      console.log('Visual Editor: Sent message to parent', type, data);
    }
  }

  // 鼠标悬浮事件
  function handleMouseOver(e) {
    if (!isEditMode) return;

    const target = e.target;
    if (target === document.body || target === document.documentElement || target.id === 'visual-editor-styles') return;

    // 移除之前的悬浮样式
    if (currentHoveredElement && currentHoveredElement !== target) {
      currentHoveredElement.classList.remove('visual-editor-hover');
    }

    // 添加悬浮样式
    if (!target.classList.contains('visual-editor-selected')) {
      target.classList.add('visual-editor-hover');
    }
    currentHoveredElement = target;
  }

  // 鼠标移出事件
  function handleMouseOut(e) {
    if (!isEditMode) return;

    const target = e.target;
    if (!target.classList.contains('visual-editor-selected')) {
      target.classList.remove('visual-editor-hover');
    }

    if (currentHoveredElement === target) {
      currentHoveredElement = null;
    }
  }

  // 点击事件
  function handleClick(e) {
    if (!isEditMode) return;

    const target = e.target;
    if (target === document.body || target === document.documentElement || target.id === 'visual-editor-styles') return;

    e.preventDefault();
    e.stopPropagation();

    const elementInfo = getElementInfo(target);

    // 切换选中状态
    const existingIndex = selectedElements.findIndex(el => el.xpath === elementInfo.xpath);

    if (existingIndex >= 0) {
      // 取消选中
      target.classList.remove('visual-editor-selected');
      selectedElements.splice(existingIndex, 1);
      console.log('Visual Editor: Element deselected', elementInfo.xpath);
    } else {
      // 添加选中
      target.classList.add('visual-editor-selected');
      target.classList.remove('visual-editor-hover');
      selectedElements.push(elementInfo);
      console.log('Visual Editor: Element selected', elementInfo.xpath);
    }

    // 发送选中的元素信息到父窗口
    sendToParent('${MESSAGE_TYPE.ELEMENT_SELECTED}', {
      elements: selectedElements,
      currentElement: elementInfo,
      isSelected: existingIndex < 0
    });
  }

  // 进入编辑模式
  function enterEditMode() {
    isEditMode = true;
    document.body.style.cursor = 'crosshair';
    console.log('Visual Editor: Entered edit mode');

    // 恢复之前选中的元素样式
    selectedElements.forEach(el => {
      try {
        const element = document.evaluate(el.xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        if (element) {
          element.classList.add('visual-editor-selected');
        }
      } catch (e) {
        console.log('Visual Editor: Could not restore selection for', el.xpath);
      }
    });
  }

  // 退出编辑模式
  function exitEditMode() {
    isEditMode = false;
    document.body.style.cursor = '';

    // 移除所有样式
    document.querySelectorAll('.visual-editor-hover').forEach(el => {
      el.classList.remove('visual-editor-hover');
    });
    document.querySelectorAll('.visual-editor-selected').forEach(el => {
      el.classList.remove('visual-editor-selected');
    });

    currentHoveredElement = null;
    console.log('Visual Editor: Exited edit mode');
  }

  // 清除选中元素
  function clearSelectedElements() {
    document.querySelectorAll('.visual-editor-selected').forEach(el => {
      el.classList.remove('visual-editor-selected');
    });
    selectedElements = [];
  }

  // 监听来自父窗口的消息
  window.addEventListener('message', (e) => {
    const { type, data } = e.data || {};
    console.log('Visual Editor: Received message', type, data);

    switch (type) {
      case '${MESSAGE_TYPE.ENTER_EDIT_MODE}':
        enterEditMode();
        break;
      case '${MESSAGE_TYPE.EXIT_EDIT_MODE}':
        exitEditMode();
        break;
      case '${MESSAGE_TYPE.REMOVE_SELECTED_ELEMENT}':
        if (data?.xpath) {
          const index = selectedElements.findIndex(el => el.xpath === data.xpath);
          if (index >= 0) {
            try {
              const element = document.evaluate(data.xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
              if (element) {
                element.classList.remove('visual-editor-selected');
              }
            } catch (e) {
              console.log('Visual Editor: Could not remove selection');
            }
            selectedElements.splice(index, 1);
          }
        }
        break;
    }
  });

  // 添加事件监听
  document.addEventListener('mouseover', handleMouseOver, true);
  document.addEventListener('mouseout', handleMouseOut, true);
  document.addEventListener('click', handleClick, true);

  // 通知父窗口脚本已准备好
  sendToParent('${MESSAGE_TYPE.SCRIPT_READY}', { ready: true });
})();
`

/**
 * 可视化编辑器类
 */
export class VisualEditor {
  private isEditMode: boolean = false
  private selectedElements: SelectedElement[] = []
  private iframe: HTMLIFrameElement | null = null
  private onElementSelected: ((elements: SelectedElement[]) => void) | null = null
  private messageHandler: ((event: MessageEvent) => void) | null = null
  private isScriptReady: boolean = false

  constructor() {
    this.messageHandler = this.handleMessage.bind(this)
    window.addEventListener('message', this.messageHandler)
    console.log('Visual Editor: Constructor called')
  }

  /**
   * 设置 iframe 引用
   */
  setIframe(iframe: HTMLIFrameElement | null) {
    console.log('Visual Editor: setIframe called', iframe?.src)
    this.iframe = iframe
    this.isScriptReady = false

    if (iframe) {
      // 移除旧的 load 监听器
      iframe.removeEventListener('load', this.handleIframeLoad)
      // 添加新的 load 监听器
      iframe.addEventListener('load', this.handleIframeLoad.bind(this))

      // 如果 iframe 已经加载完成，立即注入
      try {
        if (iframe.contentDocument?.readyState === 'complete') {
          console.log('Visual Editor: Iframe already loaded, injecting script')
          this.injectScript()
        }
      } catch (e) {
        console.log('Visual Editor: Cannot access iframe contentDocument yet')
      }
    }
  }

  /**
   * 处理 iframe 加载完成
   */
  private handleIframeLoad() {
    console.log('Visual Editor: Iframe loaded')
    this.injectScript()
  }

  /**
   * 设置元素选中回调
   */
  setOnElementSelected(callback: (elements: SelectedElement[]) => void) {
    this.onElementSelected = callback
  }

  /**
   * 注入脚本到 iframe
   */
  private injectScript() {
    if (!this.iframe) {
      console.log('Visual Editor: No iframe to inject')
      return
    }

    try {
      const doc = this.iframe.contentDocument
      if (!doc) {
        console.log('Visual Editor: No contentDocument')
        return
      }

      // 检查是否已经注入
      if (doc.querySelector('#visual-editor-script')) {
        console.log('Visual Editor: Script already exists')
        return
      }

      const script = doc.createElement('script')
      script.id = 'visual-editor-script'
      script.textContent = getInjectScript()

      // 添加到 body 或 head
      if (doc.body) {
        doc.body.appendChild(script)
      } else if (doc.head) {
        doc.head.appendChild(script)
      }

      console.log('Visual Editor: Script injected')
    } catch (e) {
      console.error('Visual Editor: Failed to inject script', e)
    }
  }

  /**
   * 处理来自 iframe 的消息
   */
  private handleMessage(event: MessageEvent) {
    const { type, data } = event.data || {}

    if (!type || !type.startsWith('VISUAL_EDITOR_')) return

    console.log('Visual Editor: Received message from iframe', type, data)

    switch (type) {
      case MESSAGE_TYPE.SCRIPT_READY:
        this.isScriptReady = true
        // 如果之前已经在编辑模式，重新发送进入编辑模式的命令
        if (this.isEditMode) {
          this.sendMessageToIframe(MESSAGE_TYPE.ENTER_EDIT_MODE)
        }
        break

      case MESSAGE_TYPE.ELEMENT_SELECTED:
        this.selectedElements = data.elements || []
        console.log('Visual Editor: Elements updated', this.selectedElements)
        if (this.onElementSelected) {
          this.onElementSelected(this.selectedElements)
        }
        break
    }
  }

  /**
   * 进入编辑模式
   */
  enterEditMode() {
    console.log('Visual Editor: enterEditMode called')
    this.isEditMode = true
    this.sendMessageToIframe(MESSAGE_TYPE.ENTER_EDIT_MODE)
  }

  /**
   * 退出编辑模式
   */
  exitEditMode() {
    console.log('Visual Editor: exitEditMode called')
    this.isEditMode = false
    this.sendMessageToIframe(MESSAGE_TYPE.EXIT_EDIT_MODE)
  }

  /**
   * 切换编辑模式
   */
  toggleEditMode(): boolean {
    if (this.isEditMode) {
      this.exitEditMode()
    } else {
      this.enterEditMode()
    }
    return this.isEditMode
  }

  /**
   * 获取当前是否处于编辑模式
   */
  getIsEditMode(): boolean {
    return this.isEditMode
  }

  /**
   * 获取选中的元素
   */
  getSelectedElements(): SelectedElement[] {
    return this.selectedElements
  }

  /**
   * 移除选中的元素
   */
  removeSelectedElement(xpath: string) {
    const index = this.selectedElements.findIndex(el => el.xpath === xpath)
    if (index >= 0) {
      this.selectedElements.splice(index, 1)
      this.sendMessageToIframe(MESSAGE_TYPE.REMOVE_SELECTED_ELEMENT, { xpath })
      if (this.onElementSelected) {
        this.onElementSelected(this.selectedElements)
      }
    }
  }

  /**
   * 清除所有选中的元素
   */
  clearSelectedElements() {
    this.selectedElements = []
    this.sendMessageToIframe(MESSAGE_TYPE.EXIT_EDIT_MODE)
    this.sendMessageToIframe(MESSAGE_TYPE.ENTER_EDIT_MODE)
    if (this.onElementSelected) {
      this.onElementSelected(this.selectedElements)
    }
  }

  /**
   * 发送消息到 iframe
   */
  private sendMessageToIframe(type: string, data?: any) {
    if (this.iframe?.contentWindow) {
      this.iframe.contentWindow.postMessage({ type, data }, '*')
      console.log('Visual Editor: Sent message to iframe', type, data)
    } else {
      console.log('Visual Editor: No iframe to send message to')
    }
  }

  /**
   * 销毁编辑器
   */
  destroy() {
    this.exitEditMode()
    this.selectedElements = []
    if (this.messageHandler) {
      window.removeEventListener('message', this.messageHandler)
    }
  }
}

/**
 * 格式化元素信息为可读文本
 */
export function formatElementInfo(element: SelectedElement): string {
  const parts: string[] = []

  parts.push(`标签: ${element.tagName}`)

  if (element.id) {
    parts.push(`ID: ${element.id}`)
  }

  if (element.className) {
    parts.push(`类名: ${element.className}`)
  }

  if (element.text) {
    parts.push(`文本: "${element.text.slice(0, 50)}${element.text.length > 50 ? '...' : ''}"`)
  }

  if (Object.keys(element.attributes).length > 0) {
    const attrs = Object.entries(element.attributes)
      .filter(([key]) => !['id', 'class'].includes(key))
      .slice(0, 3)
      .map(([key, value]) => `${key}="${value}"`)
      .join(', ')
    if (attrs) {
      parts.push(`属性: ${attrs}`)
    }
  }

  return parts.join(' | ')
}

/**
 * 生成元素描述提示词
 */
export function generateElementPrompt(elements: SelectedElement[]): string {
  if (elements.length === 0) return ''

  const descriptions = elements.map((el, index) => {
    const parts: string[] = []
    parts.push(`【元素${index + 1}】`)
    parts.push(`标签: ${el.tagName}`)

    if (el.id) {
      parts.push(`ID: #${el.id}`)
    }

    if (el.className) {
      parts.push(`类名: .${el.className.split(' ').join('.')}`)
    }

    if (el.text) {
      parts.push(`文本: "${el.text.slice(0, 100)}${el.text.length > 100 ? '...' : ''}"`)
    }

    if (Object.keys(el.attributes).length > 0) {
      const attrs = Object.entries(el.attributes)
        .filter(([key]) => !['id', 'class', 'style'].includes(key))
        .slice(0, 5)
        .map(([key, value]) => `${key}="${value}"`)
        .join(', ')
      if (attrs) {
        parts.push(`其他属性: ${attrs}`)
      }
    }

    parts.push(`CSS选择器: ${el.cssSelector}`)

    return parts.join('\n')
  })

  return `

--- 用户选中的页面元素 ---
${descriptions.join('\n\n')}
--- 请针对以上选中的元素执行用户的修改要求 ---
`
}

// 导出单例
export const visualEditor = new VisualEditor()
