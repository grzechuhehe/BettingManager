import { useState } from 'react';
import { trackNewProfile } from '../../api';

export default function TrackProfileForm({ onProfileAdded }) {
    const [username, setUsername] = useState('');
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [error, setError] = useState(null);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage(null);
        setError(null);
        try {
            const res = await trackNewProfile(username);
            setMessage(res.data);
            setUsername('');
            if (onProfileAdded) onProfileAdded();
        } catch (err) {
            setError(err.response?.data || 'An error occurred while adding the profile.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 rounded-lg shadow-md mb-6">
            <h2 className="text-xl font-bold mb-4 text-gray-800">Track New X Profile</h2>
            <form onSubmit={handleSubmit} className="flex gap-4">
                <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="@username"
                    required
                    className="flex-1 border border-gray-300 rounded px-4 py-2 focus:outline-none focus:border-blue-500"
                />
                <button 
                    type="submit" 
                    disabled={loading}
                    className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
                >
                    {loading ? 'Tracking...' : 'Track Profile'}
                </button>
            </form>
            {message && <p className="text-green-600 mt-2">{message}</p>}
            {error && <p className="text-red-600 mt-2">{error}</p>}
        </div>
    );
}
