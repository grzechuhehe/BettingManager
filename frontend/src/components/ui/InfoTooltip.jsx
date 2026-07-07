import React, { useId } from 'react';

export default function InfoTooltip({ text, className = '' }) {
    const id = useId();

    return (
        <span className={`relative inline-flex group ${className}`}>
            <button
                type="button"
                aria-describedby={id}
                className="w-4 h-4 rounded-full border border-hairline text-[10px] font-black text-muted hover:text-primary hover:border-primary/50 transition-colors flex items-center justify-center leading-none"
            >
                i
            </button>
            <span
                id={id}
                role="tooltip"
                className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 w-56 p-3 bg-surface-elevated border border-hairline rounded-lg text-[10px] text-muted font-medium normal-case tracking-normal leading-relaxed opacity-0 pointer-events-none group-hover:opacity-100 group-focus-within:opacity-100 transition-opacity shadow-2xl z-30"
            >
                {text}
            </span>
        </span>
    );
}
