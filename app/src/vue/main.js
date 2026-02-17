import { createApp } from 'vue/dist/vue.esm-bundler.js';
import '../app.css';
import { createGnssVueApp } from './vueApp.js';

createApp(createGnssVueApp()).mount('#app');
