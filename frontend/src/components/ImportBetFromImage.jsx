import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { importBetFromImage } from '../api';

const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif'];

function clipboardItemToFile(item) {
  const blob = item.getAsFile();
  if (!blob) return null;
  const ext = blob.type.split('/')[1] || 'png';
  return new File([blob], `screenshot-${Date.now()}.${ext}`, { type: blob.type || 'image/png' });
}

function fileFromDataTransfer(dataTransfer) {
  if (!dataTransfer) return null;
  const dropped = dataTransfer.files?.[0];
  if (dropped?.type.startsWith('image/')) return dropped;

  for (const item of dataTransfer.items ?? []) {
    if (item.kind === 'file' && item.type.startsWith('image/')) {
      return clipboardItemToFile(item);
    }
  }
  return null;
}

export default function ImportBetFromImage({ onImported }) {
  const containerRef = useRef(null);
  const fileInputRef = useRef(null);
  const [file, setFile] = useState(null);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);

  const previewUrl = useMemo(
    () => (file ? URL.createObjectURL(file) : null),
    [file],
  );

  useEffect(() => () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  const selectFile = useCallback((nextFile) => {
    if (!nextFile?.type.startsWith('image/')) {
      setError('Wgraj obraz (PNG/JPG) lub wklej zrzut ekranu.');
      return;
    }
    setFile(nextFile);
    setError(null);
    setSuccess(null);
  }, []);

  const clearFile = useCallback(() => {
    setFile(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, []);

  const handlePaste = useCallback((e) => {
    const items = e.clipboardData?.items;
    if (!items) return;

    for (const item of items) {
      if (item.type.startsWith('image/')) {
        e.preventDefault();
        const pastedFile = clipboardItemToFile(item);
        if (pastedFile) selectFile(pastedFile);
        return;
      }
    }
  }, [selectFile]);

  useEffect(() => {
    const onDocumentPaste = (e) => {
      const active = document.activeElement;
      const inImportSection =
        containerRef.current?.contains(active) || containerRef.current?.contains(e.target);
      if (!inImportSection) return;
      handlePaste(e);
    };

    document.addEventListener('paste', onDocumentPaste);
    return () => document.removeEventListener('paste', onDocumentPaste);
  }, [handlePaste]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Wklej zrzut ekranu (Ctrl+V) lub wybierz zdjęcie kuponu.');
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const { data } = await importBetFromImage(file, note);
      clearFile();
      setNote('');
      setSuccess(`Zaimportowano: ${data.eventName} (${data.selection})`);
      if (onImported) onImported(data);
    } catch (err) {
      const status = err?.response?.status;
      if (status === 422) {
        setError('Nie udało się odczytać zakładu ze zdjęcia. Spróbuj wyraźniejszy zrzut.');
      } else if (status === 400) {
        setError('Nieprawidłowy plik — wgraj obraz (PNG/JPG).');
      } else {
        setError('Błąd importu. Spróbuj ponownie.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div
        ref={containerRef}
        tabIndex={0}
        onPaste={handlePaste}
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragOver(true);
        }}
        onDragLeave={(e) => {
          if (!e.currentTarget.contains(e.relatedTarget)) setIsDragOver(false);
        }}
        onDrop={(e) => {
          e.preventDefault();
          setIsDragOver(false);
          const droppedFile = fileFromDataTransfer(e.dataTransfer);
          if (droppedFile) selectFile(droppedFile);
        }}
        className={`rounded-lg border-2 border-dashed p-6 transition-colors outline-none focus-visible:ring-2 focus-visible:ring-primary/50 ${
          isDragOver
            ? 'border-primary bg-primary/5'
            : file
              ? 'border-primary/40 bg-surface-soft/50'
              : 'border-hairline bg-surface-soft/30 hover:border-primary/30'
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept={IMAGE_TYPES.join(',')}
          onChange={(e) => {
            const chosen = e.target.files?.[0];
            if (chosen) selectFile(chosen);
          }}
          className="sr-only"
        />

        {previewUrl ? (
          <div className="space-y-4">
            <img
              src={previewUrl}
              alt="Podgląd kuponu"
              className="mx-auto max-h-64 rounded-md border border-hairline object-contain"
            />
            <div className="flex flex-wrap items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="text-sm font-bold uppercase tracking-wider text-muted hover:text-on-dark"
              >
                Zmień plik
              </button>
              <span className="text-muted">·</span>
              <button
                type="button"
                onClick={clearFile}
                className="text-sm font-bold uppercase tracking-wider text-rose-500 hover:text-rose-400"
              >
                Usuń
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-3 text-center">
            <p className="text-sm font-bold text-on-dark">
              Wklej zrzut ekranu ze schowka
            </p>
            <p className="text-xs text-muted">
              Kliknij tutaj i użyj <kbd className="rounded border border-hairline px-1.5 py-0.5 font-mono text-[11px]">Ctrl+V</kbd>
              {' '}albo przeciągnij obraz. Możesz też wybrać zapisany plik.
            </p>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="mt-2 text-sm font-bold uppercase tracking-wider text-primary hover:text-primary/80"
            >
              Wybierz plik
            </button>
          </div>
        )}
      </div>

      <input
        type="text"
        placeholder="Notatka (opcjonalnie)"
        value={note}
        onChange={(e) => setNote(e.target.value)}
        className="input-field w-full"
      />
      <button type="submit" disabled={loading || !file} className="button-primary w-full">
        {loading ? 'Analizuję...' : 'Importuj zakład ze zdjęcia'}
      </button>
      {error && <p className="text-rose-500 text-sm font-medium">{error}</p>}
      {success && <p className="text-primary text-sm font-medium">{success}</p>}
    </form>
  );
}
