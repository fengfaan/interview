import { marked } from 'marked'

export function normalizeMarkdown(markdown: string) {
  return markdown
    .replace(/\r\n/g, '\n')
    .replace(/([^\n])(\n?#{1,6})([^\s#])/g, '$1\n\n$2 $3')
    .replace(/^(#{1,6})([^\s#])/gm, '$1 $2')
    .replace(/\n{3,}/g, '\n\n')
}

export function renderMarkdown(markdown: string) {
  return marked(normalizeMarkdown(markdown)) as string
}
