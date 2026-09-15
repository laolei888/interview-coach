import { marked } from 'marked'

marked.setOptions({ breaks: true, gfm: true })

export function renderMarkdown(text) {
  if (!text) return ''

  let cleaned = text
    // 去评分
    .replace(/\[评分[：:]*\s*\d+\s*\/\s*\d+\]/g, '')
    .replace(/评分[：:]\s*\d+\s*\/\s*\d+/g, '')
    .replace(/\[评分[：:]*\s*\d+\s*\/\s*\d+\]/g, '')
    .replace(/[-—]{1,3}\s*(你|你的回答|候选人).*$/gm, '')
    .trim()

  // **text** → <strong>text</strong>
  cleaned = cleaned.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')

  // 每个 ## / ### 标题前保证有两个换行
  cleaned = cleaned.replace(/(#{2,3})\s*/g, '\n\n$1 ')
  cleaned = cleaned.replace(/\n{3,}/g, '\n\n')

  return marked.parse(cleaned)
}

export function stripScore(text) {
  if (!text) return ''
  return text
    .replace(/\[评分[：:]*\s*\d+\s*\/\s*\d+\]/g, '')
    .replace(/评分[：:]\s*\d+\s*\/\s*\d+/g, '')
    .replace(/[-—]{1,3}\s*(你|你的回答|候选人).*$/gm, '')
    .trim()
}
