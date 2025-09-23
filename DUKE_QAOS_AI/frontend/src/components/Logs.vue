<!--
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * Logs.vue: Displays system logs and performance metrics.
 * Features: Real-time log updates via SSE, performance chart.
 * Security: Sanitizes log display to prevent XSS.
-->
<template>
  <div class="card">
    <h2>System Logs & Performance</h2>
    <div class="controls">
      <button @click="refreshLogs">Refresh Logs</button>
      <button @click="refreshPerformance">Refresh Performance</button>
    </div>
    <div class="row">
      <div class="col" style="flex: 2">
        <div class="muted small">Recent System Logs</div>
        <div class="logs" id="log-window">
          <div v-for="log in logs" :key="log.id" class="log-entry">{{ log.timestamp }} - {{ log.level }}: {{ log.message }}</div>
        </div>
      </div>
      <div class="col" style="width: 300px">
        <div class="muted small">Performance Metrics</div>
        <canvas id="perf-chart" width="280" height="140"></canvas>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'Logs',
  data() {
    return {
      logs: [],
      performanceData: {},
      sse: null
    };
  },
  mounted() {
    this.refreshLogs();
    this.refreshPerformance();
    this.setupSSE();
    // Refresh every 10 seconds
    setInterval(() => {
      this.refreshLogs();
      this.refreshPerformance();
    }, 10000);
  },
  beforeUnmount() {
    if (this.sse) {
      this.sse.close();
    }
  },
  methods: {
    async refreshLogs() {
      try {
        const response = await axios.get('http://localhost:8080/api/logs');
        this.logs = response.data.logs.slice(-20).reverse().map((log, index) => ({
          id: index,
          timestamp: new Date().toLocaleTimeString(),
          level: log.includes('WARNING') ? 'WARNING' : log.includes('SEVERE') ? 'SEVERE' : 'INFO',
          message: this.sanitizeLog(log)
        }));
      } catch (error) {
        console.error('Logs error:', error);
        this.logs = [{ id: 0, timestamp: new Date().toLocaleTimeString(), level: 'ERROR', message: 'Failed to fetch logs' }];
      }
    },
    async refreshPerformance() {
      try {
        const response = await axios.get('http://localhost:8080/api/performance');
        this.performanceData = response.data;
        this.$nextTick(() => this.drawPerfChart());
      } catch (error) {
        console.error('Performance error:', error);
        this.performanceData = {};
      }
    },
    setupSSE() {
      try {
        this.sse = new EventSource('http://localhost:8080/events');
        this.sse.addEventListener('log', (ev) => {
          try {
            const data = JSON.parse(ev.data);
            this.logs.unshift({
              id: Date.now(),
              timestamp: new Date().toLocaleTimeString(),
              level: data.level,
              message: this.sanitizeLog(data.message)
            });
            if (this.logs.length > 50) {
              this.logs = this.logs.slice(0, 50);
            }
          } catch (e) {
            console.error('SSE parse error:', e);
          }
        });
        this.sse.addEventListener('connected', (ev) => {
          console.log('SSE connected:', ev.data);
        });
        this.sse.onerror = (e) => {
          console.error('SSE error:', e);
          this.sse.close();
          setTimeout(() => this.setupSSE(), 5000);
        };
      } catch (error) {
        console.error('SSE not supported:', error);
      }
    },
    sanitizeLog(message) {
      // Prevent XSS in log display
      const div = document.createElement('div');
      div.textContent = message;
      return div.innerHTML;
    },
    drawPerfChart() {
      const canvas = document.getElementById('perf-chart');
      if (!canvas || Object.keys(this.performanceData).length === 0) return;
      
      const ctx = canvas.getContext('2d');
      const w = canvas.width;
      const h = canvas.height;
      
      // Clear canvas
      ctx.fillStyle = 'rgba(255,255,255,0.02)';
      ctx.fillRect(0, 0, w, h);
      
      const keys = Object.keys(this.performanceData);
      if (keys.length === 0) {
        ctx.fillStyle = '#9aa6b2';
        ctx.font = '12px monospace';
        ctx.fillText('No perf data', 10, 20);
        return;
      }
      
      const vals = keys.map(k => this.performanceData[k]);
      const max = Math.max(...vals, 1);
      const barW = Math.floor(w / keys.length) - 6;
      
      keys.forEach((k, i) => {
        const v = this.performanceData[k];
        const bh = Math.max(4, Math.floor((v / max) * (h - 20)));
        const x = i * (barW + 6) + 6;
        const y = h - bh - 6;
        
        // Color based on performance
        const color = v > 100 ? 'rgba(255, 92, 92, 0.8)' : 'rgba(124, 92, 255, 0.95)';
        ctx.fillStyle = color;
        ctx.fillRect(x, y, barW, bh);
        
        // Label
        ctx.fillStyle = '#9aa6b2';
        ctx.font = '10px monospace';
        ctx.fillText(k.substring(0, 8), x, h - 2);
        ctx.fillText(v + 'ms', x, y - 2);
      });
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
.controls {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
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
.row {
  display: flex;
  gap: 10px;
}
.col {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.logs {
  height: 200px;
  overflow: auto;
  font-family: monospace;
  background: linear-gradient(180deg, rgba(255,255,255,0.02), transparent);
  padding: 8px;
  border-radius: 8px;
  border: 1px solid rgba(255,255,255,0.04);
}
.log-entry {
  font-size: 11px;
  margin-bottom: 2px;
  white-space: pre-wrap;
  word-break: break-all;
}
.muted {
  color: #9aa6b2;
}
.small {
  font-size: 12px;
}
</style>
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */