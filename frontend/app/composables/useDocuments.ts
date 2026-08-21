import type { DocumentFile } from '~/types/api'

/**
 * The shared rules for uploading documents, in one place because four screens post to four
 * different endpoints with exactly the same constraints.
 *
 * The numbers are not decoration — they mirror what the backend enforces:
 *
 *  - 10 MB per file      `FileStorageService.MAX_FILE_SIZE` and `spring.servlet.multipart.max-file-size`
 *  - 4 files per call    `DocumentService`'s batch cap. 4 x 10 MB is exactly `max-request-size`
 *                        (40 MB) and sits under nginx's 45 M, which is why a 20-file selection
 *                        is chunked into five calls rather than the limits being raised.
 *  - 20 per owner        `DocumentService`'s per-owner cap; checked here only to fail fast with
 *                        a Persian message instead of spending an upload on a 409.
 */
export const DOCUMENT_BATCH_SIZE = 4
export const DOCUMENT_MAX_PER_OWNER = 20
export const DOCUMENT_MAX_FILE_BYTES = 10 * 1024 * 1024

/** Extensions and their MIME types together: some browsers filter on one, some on the other. */
export const DOCUMENT_ACCEPT = [
  '.jpg', '.jpeg', '.png', '.webp', '.pdf', '.xlsx', '.docx',
  'image/jpeg',
  'image/png',
  'image/webp',
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
].join(',')

export const DOCUMENT_ACCEPT_LABEL = 'JPG، PNG، WebP، PDF، DOCX یا XLSX — حداکثر ۱۰ مگابایت برای هر فایل.'

/**
 * Files picked but not yet posted, because the owning record does not exist yet.
 *
 * The creation endpoints stay JSON (their Bean Validation messages are Persian and are
 * surfaced per field), so a document can only be attached once the request or centre has an
 * id. Until then the files sit here.
 */
export interface StagedDocumentBatch {
  categoryId: number
  categoryName: string
  /** Honoured by the backend only when the whole upload is a single file. */
  title: string | null
  files: File[]
}

type ApiFetch = ReturnType<typeof useNuxtApp>['$api']

/** Splits a selection into calls the backend will accept. */
export function chunkDocuments<T>(items: T[], size = DOCUMENT_BATCH_SIZE): T[][] {
  const chunks: T[][] = []
  for (let i = 0; i < items.length; i += size) chunks.push(items.slice(i, i + size))
  return chunks
}

/** The lower-cased extension, for the badge and nothing else. Never used to build a class name. */
export function documentExtension(doc: Pick<DocumentFile, 'originalFilename' | 'url'>): string {
  const source = doc.originalFilename || doc.url || ''
  const match = /\.([a-z0-9]+)(?:$|\?)/i.exec(source)
  return match?.[1]?.toLowerCase() ?? 'file'
}

/** What a document is called on screen: its title, else the name it was uploaded under. */
export function documentLabel(doc: DocumentFile): string {
  return doc.title || doc.originalFilename || 'سند بدون نام'
}

/**
 * Client-side pre-flight. The backend checks all of this again — this only turns the common
 * mistakes into a Persian sentence before 40 MB goes over a mobile connection.
 */
export function validateDocumentSelection(files: File[], existingCount: number): string | null {
  if (!files.length) return null
  const tooBig = files.find(file => file.size > DOCUMENT_MAX_FILE_BYTES)
  if (tooBig) return `حجم «${tooBig.name}» بیشتر از ۱۰ مگابایت است.`
  if (existingCount + files.length > DOCUMENT_MAX_PER_OWNER) {
    return `حداکثر ${DOCUMENT_MAX_PER_OWNER} مدرک قابل بارگذاری است؛ هم‌اکنون ${existingCount} مدرک ثبت شده است.`
  }
  return null
}

/**
 * Posts staged batches, four files at a time, sequentially.
 *
 * The body is a `FormData` and the Content-Type is left for the browser: setting it by hand
 * omits the multipart boundary and the upload fails. `pages/profile.vue`'s `uploadLogo` is the
 * same shape.
 *
 * Sequential rather than parallel on purpose — the per-owner cap is checked server-side as
 * `existing + incoming`, and four concurrent calls would each see the same `existing`.
 *
 * Returns the last response, which is the owning record with every document now on it.
 */
export async function uploadDocumentBatches<T>(
  api: ApiFetch,
  endpoint: string,
  batches: StagedDocumentBatch[],
  onProgress?: (done: number, total: number) => void,
): Promise<T | null> {
  const calls: { categoryId: number, title: string | null, files: File[] }[] = []

  for (const batch of batches) {
    for (const files of chunkDocuments(batch.files)) {
      calls.push({
        categoryId: batch.categoryId,
        // A title names one document. The backend honours it only when the call carries exactly
        // one file and drops it silently otherwise, so it is sent only for a single-file pick
        // that also lands in a single-file call.
        title: batch.files.length === 1 && files.length === 1 ? batch.title : null,
        files,
      })
    }
  }

  let result: T | null = null
  let done = 0
  onProgress?.(0, calls.length)

  for (const call of calls) {
    const body = new FormData()
    for (const file of call.files) body.append('files', file)
    body.append('categoryId', String(call.categoryId))
    if (call.title) body.append('title', call.title)

    result = await api<T>(endpoint, { method: 'POST', body })
    onProgress?.(++done, calls.length)
  }

  return result
}
