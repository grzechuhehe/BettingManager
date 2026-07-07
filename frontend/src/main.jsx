import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { BrowserRouter } from 'react-router-dom';
import { UISettingsProvider } from './context/UISettingsContext';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <UISettingsProvider>
        <App />
      </UISettingsProvider>
    </BrowserRouter>
  </StrictMode>,
)
