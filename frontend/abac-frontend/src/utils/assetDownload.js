import { exportAssetReference } from '@/api/asset'

export async function triggerAssetDownload(assetId) {
  const result = await exportAssetReference(assetId)
  const downloadUrl = result.downloadUrl || result.filePath

  if (!downloadUrl) {
    throw new Error('后端没有返回可用的下载地址')
  }

  window.open(downloadUrl, '_blank', 'noopener')
  return result
}
