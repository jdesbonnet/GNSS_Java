export class TextLogPanel {
  constructor({ container }) {
    this.container = container;
  }

  update({ lines, sentenceFilter, constellations }) {
    this.container.innerHTML = '';
    const wrapper = document.createElement('div');
    (lines || []).slice(-180).forEach((line) => {
      const talker = line.talker || '??';
      const group = line.group || 'OTHER';
      if (!sentenceFilter[group]) return;
      if (talker in constellations && !constellations[talker]) return;
      const row = document.createElement('div');
      row.className = 'log-row';
      row.textContent = `${line.lineNo ?? '-'}: ${line.line}`;
      if (line.error) row.style.color = '#b91c1c';
      wrapper.appendChild(row);
    });
    this.container.appendChild(wrapper);
  }
}
