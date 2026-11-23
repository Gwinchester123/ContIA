import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { createHash } from 'node:crypto'
import { readdirSync, readFileSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const frontendRoot = dirname(fileURLToPath(import.meta.url))

function contiaPwaPlugin() {
  return {
    name: 'contia-pwa-service-worker',
    apply: 'build',
    generateBundle(_options, bundle) {
      const hash = createHash('sha256')
      const arquivos = []
      const publicDir = resolve(frontendRoot, 'public')

      const incluirArquivosPublicos = (diretorio) => {
        for (const entrada of readdirSync(diretorio, { withFileTypes: true })) {
          const caminho = join(diretorio, entrada.name)
          if (entrada.isDirectory()) incluirArquivosPublicos(caminho)
          else arquivos.push([`public/${relative(publicDir, caminho).replaceAll('\\', '/')}`, readFileSync(caminho)])
        }
      }

      incluirArquivosPublicos(publicDir)
      for (const [nome, arquivo] of Object.entries(bundle)) {
        if (nome !== 'sw.js') arquivos.push([nome, arquivo.type === 'asset' ? arquivo.source : arquivo.code])
      }
      arquivos.sort(([nomeA], [nomeB]) => nomeA.localeCompare(nomeB))
      for (const [nome, conteudo] of arquivos) {
        hash.update(nome)
        hash.update(Buffer.isBuffer(conteudo) ? conteudo : String(conteudo))
      }

      const versao = hash.digest('hex').slice(0, 12)
      const modelo = readFileSync(resolve(frontendRoot, 'pwa/sw.js'), 'utf8')
      this.emitFile({
        type: 'asset',
        fileName: 'sw.js',
        source: modelo.replaceAll('__APP_VERSION__', versao),
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), contiaPwaPlugin()],
})
