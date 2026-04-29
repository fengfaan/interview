import http from 'node:http'
import { pipeline, env } from '@huggingface/transformers'

const HOST = process.env.SMART_EMBEDDING_HOST || '127.0.0.1'
const PORT = Number(process.env.SMART_EMBEDDING_PORT || 8765)
const MODEL = process.env.SMART_EMBEDDING_MODEL || 'TaylorAI/bge-micro-v2'
const DTYPE = process.env.SMART_EMBEDDING_DTYPE || 'q8'
const MAX_BODY_BYTES = 1024 * 1024

env.allowLocalModels = false

let extractorPromise

function getExtractor() {
  if (!extractorPromise) {
    extractorPromise = pipeline('feature-extraction', MODEL, {
      dtype: DTYPE,
      quantized: true,
    })
  }
  return extractorPromise
}

async function embed(text) {
  const extractor = await getExtractor()
  const output = await extractor(text, {
    pooling: 'mean',
    normalize: true,
  })
  const vector = Array.from(output.data).map((value) => Math.round(value * 1e8) / 1e8)
  return {
    model: MODEL,
    dims: vector.length,
    vector,
  }
}

function sendJson(res, status, body) {
  const payload = JSON.stringify(body)
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(payload),
  })
  res.end(payload)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let size = 0
    const chunks = []
    req.on('data', (chunk) => {
      size += chunk.length
      if (size > MAX_BODY_BYTES) {
        reject(new Error('Request body too large'))
        req.destroy()
        return
      }
      chunks.push(chunk)
    })
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')))
    req.on('error', reject)
  })
}

async function handle(req, res) {
  const url = new URL(req.url, `http://${HOST}:${PORT}`)
  if (req.method === 'GET' && url.pathname === '/health') {
    sendJson(res, 200, {
      ok: true,
      model: MODEL,
      loaded: Boolean(extractorPromise),
    })
    return
  }

  if (req.method === 'POST' && url.pathname === '/embed') {
    try {
      const bodyText = await readBody(req)
      const body = bodyText ? JSON.parse(bodyText) : {}
      const text = typeof body.text === 'string' ? body.text.trim() : ''
      if (!text) {
        sendJson(res, 400, { error: 'text is required' })
        return
      }
      sendJson(res, 200, await embed(text))
    } catch (error) {
      sendJson(res, 500, { error: error.message || 'embedding failed' })
    }
    return
  }

  sendJson(res, 404, { error: 'not found' })
}

const server = http.createServer((req, res) => {
  handle(req, res).catch((error) => sendJson(res, 500, { error: error.message || 'server error' }))
})

server.listen(PORT, HOST, () => {
  console.log(`Smart embedding sidecar listening on http://${HOST}:${PORT}`)
  console.log(`Model: ${MODEL}`)
})
