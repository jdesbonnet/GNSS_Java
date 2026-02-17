export function createNmeaParserWorker() {
  const worker = new Worker(new URL('./nmeaWorker.js', import.meta.url), { type: 'module' });

  return {
    async parseFile(file, onRecords) {
      worker.postMessage({ type: 'start' });
      return new Promise((resolve) => {
        worker.onmessage = (event) => {
          if (event.data.type === 'records') {
            onRecords(event.data.records, event.data.parserState, event.data.final);
            if (event.data.final) resolve(event.data.parserState);
          }
        };

        const CHUNK = 1024 * 1024;
        let offset = 0;
        const readNext = async () => {
          const slice = file.slice(offset, offset + CHUNK);
          const text = await slice.text();
          offset += CHUNK;
          const final = offset >= file.size;
          worker.postMessage({ type: 'chunk', text, final });
          if (!final) readNext();
        };
        readNext();
      });
    },
    dispose() {
      worker.terminate();
    }
  };
}
