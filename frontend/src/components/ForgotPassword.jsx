import React, { useState } from 'react';
import { forgotPassword } from '../api';
import { Link } from 'react-router-dom';

const ForgotPassword = () => {
    const [email, setEmail] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage('');
        setError('');
        try {
            const response = await forgotPassword(email);
            setMessage(response.data.message);
        } catch (err) {
            setError(err.response?.data?.error || 'Wystąpił błąd podczas wysyłania prośby.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
            <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
                <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">Zapomniałeś hasła?</h2>
                <p className="text-gray-600 text-center mb-6">Wpisz swój adres e-mail, a wyślemy Ci link do resetowania hasła.</p>
                
                {message && <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-4">{message}</div>}
                {error && <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="mb-4">
                        <label className="block text-gray-700 text-sm font-bold mb-2">E-mail</label>
                        <input
                            type="email"
                            required
                            className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>
                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 text-white font-bold py-2 px-4 rounded-lg hover:bg-blue-700 transition duration-200 disabled:opacity-50"
                    >
                        {loading ? 'Wysyłanie...' : 'Wyślij link'}
                    </button>
                </form>
                <div className="mt-6 text-center">
                    <Link to="/login" className="text-blue-600 hover:underline text-sm">Wróć do logowania</Link>
                </div>
            </div>
        </div>
    );
};

export default ForgotPassword;
