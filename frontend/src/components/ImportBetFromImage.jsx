import { useState } from 'react';
import { importBetFromImage } from '../api';

export default function ImportBetFromImage({ onImported }) {
  const [file, setFile] = useState(null);
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Wybierz zdjęcie kuponu.');
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const { data } = await importBetFromImage(file, note);
      setFile(null);
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
      <input
        type="file"
        accept="image/*"
        onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        className="input-field w-full"
      />
      <input
        type="text"
        placeholder="Notatka (opcjonalnie)"
        value={note}
        onChange={(e) => setNote(e.target.value)}
        className="input-field w-full"
      />
      <button type="submit" disabled={loading} className="button-primary w-full">
        {loading ? 'Analizuję...' : 'Importuj zakład ze zdjęcia'}
      </button>
      {error && <p className="text-rose-500 text-sm font-medium">{error}</p>}
      {success && <p className="text-primary text-sm font-medium">{success}</p>}
    </form>
  );
}
