import axios from 'axios';

const API = axios.create({
  baseURL: 'https://localhost:8443/api',
});

export default API;
