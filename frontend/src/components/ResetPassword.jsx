import React, { useState } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { resetPassword } from '../api';

const ResetPassword = () => {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (newPassword !== confirmPassword) {
            setError('Hasla nie sa identyczne');
            return;
        }

        setLoading(true);
        setMessage('');
        setError('');

        try {
            const response = await resetPassword(token, newPassword);
            setMessage(response.data.message);
            setTimeout(() => navigate('/login'), 3000);
        } catch (err) {
            setError(err.response?.data?.error || 'Wystapil blad podczas resetowania hasla.');
        } finally {
            setLoading(false);
        }
    };

    if (!token) {
        return (
            <div className="flex flex-col items-center justify-center py-section">
                <div className="surface-card w-full max-w-md text-center">
                    <h2 className="display-sm mb-4 text-accent-rose">Blad</h2>
                    <p className="body-sm text-muted mb-6">Nieprawidlowy lub brakujacy token resetowania.</p>
                    <Link to="/login" className="text-link">Wroc do logowania</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center justify-center py-section">
            <div className="surface-card w-full max-w-md animate-scale-in">
                <h2 className="display-sm text-center mb-4">Ustaw nowe haslo</h2>
                {message && <div className="mb-6 alert-success">{message} (Przekierowanie do logowania...)</div>}
                {error && <div className="mb-6 alert-error">{error}</div>}
                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="field-label">Nowe haslo</label>
                        <input type="password" required minLength={6} className="input-field" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
                    </div>
                    <div>
                        <label className="field-label">Potwierdz nowe haslo</label>
                        <input type="password" required minLength={6} className="input-field" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} />
                    </div>
                    <button type="submit" disabled={loading} className="button-primary w-full">{loading ? 'Przetwarzanie...' : 'Zmien haslo'}</button>
                </form>
            </div>
        </div>
    );
};

export default ResetPassword;
