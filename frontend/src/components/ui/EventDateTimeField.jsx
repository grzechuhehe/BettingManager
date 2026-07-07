import React, { useMemo } from 'react';

const pad = (n) => String(n).padStart(2, '0');

const splitValue = (value) => {
    if (!value) return { date: '', time: '' };
    const [date, time = ''] = value.split('T');
    return { date, time: time.slice(0, 5) };
};

const combineValue = (date, time) => {
    if (!date) return '';
    return `${date}T${time || '12:00'}`;
};

const formatPreview = (value) => {
    if (!value) return null;
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return null;
    return parsed.toLocaleString('en-US', {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
    });
};

const applyPreset = (preset) => {
    const now = new Date();
    const date = new Date(now);

    if (preset === 'tonight') {
        date.setHours(20, 0, 0, 0);
        if (date <= now) date.setDate(date.getDate() + 1);
    } else if (preset === 'tomorrow') {
        date.setDate(date.getDate() + 1);
        date.setHours(15, 0, 0, 0);
    } else if (preset === 'weekend') {
        const day = date.getDay();
        const daysUntilSaturday = day === 6 ? 7 : (6 - day + 7) % 7 || 7;
        date.setDate(date.getDate() + daysUntilSaturday);
        date.setHours(14, 0, 0, 0);
    }

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export default function EventDateTimeField({ value, onChange, required = false, idPrefix = 'event' }) {
    const { date, time } = splitValue(value);
    const preview = useMemo(() => formatPreview(value), [value]);

    const handleDateChange = (e) => {
        onChange(combineValue(e.target.value, time));
    };

    const handleTimeChange = (e) => {
        onChange(combineValue(date, e.target.value));
    };

    const handlePreset = (preset) => {
        onChange(applyPreset(preset));
    };

    return (
        <div className="space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                    <label htmlFor={`${idPrefix}-date`} className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">
                        Date
                    </label>
                    <input
                        id={`${idPrefix}-date`}
                        type="date"
                        value={date}
                        onChange={handleDateChange}
                        required={required}
                        className="datetime-field"
                    />
                </div>
                <div>
                    <label htmlFor={`${idPrefix}-time`} className="block text-[10px] font-bold text-muted uppercase tracking-[0.2em] mb-2">
                        Time
                    </label>
                    <input
                        id={`${idPrefix}-time`}
                        type="time"
                        value={time}
                        onChange={handleTimeChange}
                        required={required}
                        className="datetime-field"
                    />
                </div>
            </div>

            <div className="flex flex-wrap gap-2">
                {[
                    { key: 'tonight', label: 'Tonight 8 PM' },
                    { key: 'tomorrow', label: 'Tomorrow 3 PM' },
                    { key: 'weekend', label: 'Next Saturday 2 PM' },
                ].map((preset) => (
                    <button
                        key={preset.key}
                        type="button"
                        onClick={() => handlePreset(preset.key)}
                        className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-widest rounded-md border border-hairline text-muted hover:text-on-dark hover:border-primary/50 hover:bg-surface-soft transition-colors"
                    >
                        {preset.label}
                    </button>
                ))}
            </div>

            {preview && (
                <p className="text-xs text-body bg-surface-soft border border-hairline rounded-md px-3 py-2">
                    <span className="text-muted font-bold uppercase tracking-widest text-[10px] mr-2">Scheduled</span>
                    {preview}
                </p>
            )}
        </div>
    );
}
