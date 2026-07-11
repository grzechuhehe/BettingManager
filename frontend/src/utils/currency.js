const DEFAULT_CURRENCY = 'PLN';

export function resolveCurrency(currency) {
    if (!currency || typeof currency !== 'string') {
        return DEFAULT_CURRENCY;
    }
    return currency.trim().toUpperCase() || DEFAULT_CURRENCY;
}

export function formatMoney(amount, currency) {
    if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
        return '';
    }
    const code = resolveCurrency(currency);
    return `${Number(amount).toFixed(2)} ${code}`;
}

export function formatSignedMoney(amount, currency) {
    if (amount === null || amount === undefined || Number.isNaN(Number(amount))) {
        return 'N/A';
    }
    const num = Number(amount);
    const formatted = formatMoney(Math.abs(num), currency);
    if (num > 0) return `+${formatted}`;
    if (num < 0) return `-${formatted}`;
    return formatted;
}
