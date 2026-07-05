import { useEffect, useState } from 'react';

function parseNumeric(value) {
    if (typeof value === 'number' && Number.isFinite(value)) {
        return value;
    }
    const parsed = parseFloat(String(value).replace(/[^0-9.-]/g, ''));
    return Number.isFinite(parsed) ? parsed : null;
}

export default function AnimatedNumber({ value, className = '', duration = 500, format }) {
    const target = parseNumeric(value);
    const [display, setDisplay] = useState(() => target ?? 0);

    useEffect(() => {
        if (target === null) {
            return undefined;
        }

        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
            setDisplay(target);
            return undefined;
        }

        const startTime = performance.now();
        let frameId;

        const tick = (now) => {
            const progress = Math.min(1, (now - startTime) / duration);
            const eased = 1 - Math.pow(1 - progress, 3);
            setDisplay(target * eased);
            if (progress < 1) {
                frameId = requestAnimationFrame(tick);
            }
        };

        setDisplay(0);
        frameId = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(frameId);
    }, [target, duration]);

    if (target === null) {
        return <span className={className}>{value}</span>;
    }

    const formatted = format ? format(display) : String(Math.round(display));
    return <span className={className}>{formatted}</span>;
}
