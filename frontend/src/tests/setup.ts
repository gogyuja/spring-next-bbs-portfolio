import '@testing-library/jest-dom';
import axios from 'axios';

// Force Node.js HTTP adapter so MSW can intercept requests in jsdom test environment
axios.defaults.adapter = 'http';
