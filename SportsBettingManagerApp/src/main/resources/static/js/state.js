// Stan aplikacji
let state = {
    user: null,
    token: localStorage.getItem('token'),
    userId: localStorage.getItem('userId'),
    currentView: 'login'
};

export function getState() {
    return state;
}

export function setState(newState) {
    state = { ...state, ...newState };
}

export function clearState() {
    state = {
        user: null,
        token: null,
        userId: null,
        currentView: 'login'
    };
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
} 