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
            setError(err.response?.data?.error || 'Wystapil blad podczas wysylania prosby.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center py-section">
            <div className="surface-card w-full max-w-md animate-scale-in">
                <h2 className="display-sm text-center mb-4">Zapomniales hasla?</h2>
                <p className="body-sm text-muted text-center mb-8">Wpisz swoj adres e-mail, a wyslemy Ci link do resetowania hasla.</p>
                {message && <div className="mb-6 alert-success">{message}</div>}
                {error && <div className="mb-6 alert-error">{error}</div>}
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="field-label">E-mail</label>
                        <input type="email" required className="input-field" value={email} onChange={(e) => setEmail(e.target.value)} />
                    </div>
                    <button type="submit" disabled={loading} className="button-primary w-full">{loading ? 'Wysylanie...' : 'Wyslij link'}</button>
                </form>
                <div className="mt-8 text-center">
                    <Link to="/login" className="text-link body-sm">Wroc do logowania</Link>
                </div>
            </div>
        </div>
    );
};

export default ForgotPassword;
