export class SignalBarPanel {
  constructor({ container }) {
    this.container = container;
  }

  update({ satellites }) {
    this.container.innerHTML = '';
    const table = document.createElement('table');
    table.className = 'table';
    table.innerHTML = '<thead><tr><th>PRN/Band</th><th>Talker</th><th>SNR</th><th>Bar</th></tr></thead>';
    const tbody = document.createElement('tbody');

    (satellites || []).slice(0, 36).forEach((sat) => {
      const tr = document.createElement('tr');
      const snr = sat.snr ?? 0;
      tr.innerHTML = `<td>${sat.prn}/${sat.band}</td><td>${sat.talker}</td><td>${snr}</td><td><div style="width:${Math.min(100, snr * 2)}%; background:#3b82f6; color:white; text-align:right; padding-right:4px;"> </div></td>`;
      tbody.appendChild(tr);
    });
    table.appendChild(tbody);
    this.container.appendChild(table);
  }
}
