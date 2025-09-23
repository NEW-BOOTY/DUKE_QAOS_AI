<!--
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * App.vue: Main Vue.js application integrating all components.
 * Features: Real-time dashboard, task triggers, performance metrics.
-->
<template>
  <div class="app">
    <h1>DUKE QAOS-AI Dashboard</h1>
    <Duke3D />
    <QuantumUI />
    <ThreatMonitor />
    <IdentityManager />
    <Logs />
  </div>
</template>

<script>
import Duke3D from './components/Duke3D.vue';
import QuantumUI from './components/QuantumUI.vue';
import ThreatMonitor from './components/ThreatMonitor.vue';
import IdentityManager from './components/IdentityManager.vue';
import Logs from './components/Logs.vue';

export default {
  name: 'App',
  components: {
    Duke3D,
    QuantumUI,
    ThreatMonitor,
    IdentityManager,
    Logs
  }
};
</script>

<style>
.app {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
  background: #0a0a23;
  color: #fff;
}
</style>
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */