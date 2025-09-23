<!--
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * ThreatMonitor.vue: Real-time security event monitoring.
 * Features: Triggers security scans, displays event logs.
-->
<template>
  <div class="card">
    <h2>Threat Monitor</h2>
    <div class="row">
      <div class="col">
        <label class="small muted">Security Event</label>
        <input v-model="event" placeholder="Enter event" />
      </div>
      <div style="width:160px">
        <label class="small muted">Severity</label>
        <select v-model="severity">
          <option>info</option>
          <option>warning</option>
          <option>threat</option>
        </select>
      </div>
    </div>
    <div class="controls">
      <button @click="scanSecurity">Trigger Security Scan</button>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ThreatMonitor',
  data() {
    return {
      event: 'Routine Check',
      severity: 'info'
    };
  },
  methods: {
    async scanSecurity() {
      try {
        const event = `${this.event} [${this.severity}]`;
        const response = await axios.post('http://localhost:8080/api/monitor-security', { event });
        if (response.data.error) {
          alert(`Error: ${response.data.error}`);
        } else {
          this.$emit('refresh-logs');
        }
      } catch (error) {
        alert(`Error: ${error.message}`);
      }
    }
  }
};
</script>

<style scoped>
.card {
  background: #15153d;
  padding: 14px;
  border-radius: 12px;
  margin-bottom: 12px;
}
.row {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.controls {
  display: flex;
  gap: 8px;
}
button {
  background: linear-gradient(90deg, #7c5cff, #00d4ff);
  border: none;
  color: #021018;
  padding: 8px 12px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
input, select {
  background: #1b1b4d;
  border: 1px solid rgba(255,255,255,0.04);
  padding: 8px;
  border-radius: 8px;
  color: #fff;
}
.muted {
  color: #9aa6b2;
}
.small {
  font-size: 12px;
}
</style>
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */