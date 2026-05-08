import json
import os
from datetime import datetime

BASELINE_FILE = "ragas_baseline_result.json"
ADVANCED_FILE = "ragas_final_result.json"
OUTPUT_FILE = "ragas_report.html"


def load_result(path):
    if not os.path.exists(path):
        return None
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def score_color(score):
    if score is None or score != score:
        return "#888888"
    if score >= 0.8:
        return "#27ae60"
    if score >= 0.6:
        return "#f39c12"
    return "#e74c3c"


def fmt(score):
    if score is None or score != score:
        return "N/A"
    return f"{score:.4f}"


def generate_html(baseline, advanced):
    now = datetime.now().strftime("%Y-%m-%d %H:%M")

    metrics = [
        ("faithfulness",      "Faithfulness",       "답변이 컨텍스트에 충실한 정도"),
        ("answer_relevancy",  "Answer Relevancy",   "질문과 답변의 관련성"),
        ("context_precision", "Context Precision",  "검색된 컨텍스트의 정확도"),
        ("context_recall",    "Context Recall",     "필요한 컨텍스트 검색 완전성"),
    ]

    b = {k: baseline.get(k, 0) for k, _, _ in metrics} if baseline else {}
    a = {k: advanced.get(k, 0) for k, _, _ in metrics} if advanced else {}
    has_advanced = advanced is not None

    labels = [label for _, label, _ in metrics]
    b_vals = [b.get(k, 0) for k, _, _ in metrics]
    a_vals = [a.get(k, 0) for k, _, _ in metrics] if has_advanced else [0, 0, 0, 0]

    details = baseline.get("details", []) if baseline else []
    details_rows = ""
    for d in details:
        q = d.get("question", "")
        ans = d.get("answer", "")[:100] + ("..." if len(d.get("answer", "")) > 100 else "")
        gt = d.get("ground_truth", "")
        ctx_list = d.get("contexts", [d.get("context", "")])
        ctx = "<br>".join([f"[{i+1}] {c[:80]}..." for i, c in enumerate(ctx_list)])
        details_rows += f"""
        <tr>
            <td>{q}</td>
            <td>{ans}</td>
            <td>{gt}</td>
            <td>{ctx}</td>
        </tr>"""

    metric_cards = ""
    for key, label, desc in metrics:
        bv = b.get(key, 0)
        av = a.get(key, 0) if has_advanced else None
        diff = ""
        if has_advanced and av is not None:
            delta = av - bv
            sign = "+" if delta >= 0 else ""
            color = "#27ae60" if delta >= 0 else "#e74c3c"
            diff = f'<div class="diff" style="color:{color}">{sign}{delta:.4f}</div>'

        metric_cards += f"""
        <div class="metric-card">
            <div class="metric-label">{label}</div>
            <div class="metric-desc">{desc}</div>
            <div class="metric-scores">
                <div class="score-box">
                    <div class="score-title">베이스라인</div>
                    <div class="score-value" style="color:{score_color(bv)}">{fmt(bv)}</div>
                </div>
                {"" if not has_advanced else f'''
                <div class="score-box">
                    <div class="score-title">고도화 후</div>
                    <div class="score-value" style="color:{score_color(av)}">{fmt(av)}</div>
                </div>'''}
            </div>
            {diff}
        </div>"""

    advanced_badge = '<span class="badge badge-done">고도화 완료</span>' if has_advanced else '<span class="badge badge-pending">고도화 전</span>'

    html = f"""<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>RAGAS 평가 리포트</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ font-family: 'Segoe UI', sans-serif; background: #0f1117; color: #e0e0e0; min-height: 100vh; }}
  .header {{ background: linear-gradient(135deg, #1a1d2e, #252840); padding: 32px 40px; border-bottom: 1px solid #2e3150; }}
  .header h1 {{ font-size: 26px; font-weight: 700; color: #fff; }}
  .header .sub {{ font-size: 13px; color: #888; margin-top: 6px; }}
  .badge {{ display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; margin-left: 12px; vertical-align: middle; }}
  .badge-done {{ background: #1a4731; color: #27ae60; }}
  .badge-pending {{ background: #3a2a10; color: #f39c12; }}
  .container {{ max-width: 1200px; margin: 0 auto; padding: 32px 24px; }}
  .section-title {{ font-size: 16px; font-weight: 600; color: #aaa; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 20px; }}
  .metrics-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 40px; }}
  .metric-card {{ background: #1a1d2e; border: 1px solid #2e3150; border-radius: 12px; padding: 20px; }}
  .metric-label {{ font-size: 15px; font-weight: 600; color: #fff; margin-bottom: 4px; }}
  .metric-desc {{ font-size: 12px; color: #666; margin-bottom: 16px; }}
  .metric-scores {{ display: flex; gap: 16px; }}
  .score-box {{ flex: 1; }}
  .score-title {{ font-size: 11px; color: #666; margin-bottom: 4px; }}
  .score-value {{ font-size: 28px; font-weight: 700; }}
  .diff {{ font-size: 14px; font-weight: 600; margin-top: 12px; }}
  .chart-section {{ background: #1a1d2e; border: 1px solid #2e3150; border-radius: 12px; padding: 24px; margin-bottom: 40px; }}
  .chart-wrap {{ position: relative; height: 320px; }}
  .table-section {{ background: #1a1d2e; border: 1px solid #2e3150; border-radius: 12px; padding: 24px; overflow-x: auto; margin-bottom: 40px; }}
  table {{ width: 100%; border-collapse: collapse; font-size: 13px; }}
  th {{ background: #252840; color: #aaa; padding: 10px 14px; text-align: left; font-weight: 500; border-bottom: 1px solid #2e3150; }}
  td {{ padding: 10px 14px; border-bottom: 1px solid #1e2135; color: #ccc; vertical-align: top; line-height: 1.6; }}
  tr:hover td {{ background: #1e2135; }}
  .footer {{ text-align: center; padding: 24px; color: #444; font-size: 12px; }}
</style>
</head>
<body>
<div class="header">
  <h1>📊 RAGAS 평가 리포트 {advanced_badge}</h1>
  <div class="sub">멀티벤더 마켓 플랫폼 AI 챗봇 · 생성 시각: {now}</div>
</div>
<div class="container">

  <div class="section-title">지표 요약</div>
  <div class="metrics-grid">{metric_cards}</div>

  <div class="section-title">레이더 차트 비교</div>
  <div class="chart-section">
    <div class="chart-wrap"><canvas id="radarChart"></canvas></div>
  </div>

  <div class="section-title">막대 차트 비교</div>
  <div class="chart-section">
    <div class="chart-wrap"><canvas id="barChart"></canvas></div>
  </div>

  <div class="section-title">질문별 상세 (베이스라인)</div>
  <div class="table-section">
    <table>
      <thead>
        <tr>
          <th style="width:18%">질문</th>
          <th style="width:30%">챗봇 응답</th>
          <th style="width:22%">정답 (Ground Truth)</th>
          <th style="width:30%">실제 검색 컨텍스트</th>
        </tr>
      </thead>
      <tbody>{details_rows}</tbody>
    </table>
  </div>

</div>
<div class="footer">Generated by RAGAS Evaluation Pipeline · {now}</div>

<script>
const labels = {json.dumps(labels, ensure_ascii=False)};
const bVals  = {json.dumps(b_vals)};
const aVals  = {json.dumps(a_vals)};
const hasAdv = {'true' if has_advanced else 'false'};

const radarCtx = document.getElementById('radarChart').getContext('2d');
const radarDatasets = [{{
  label: '베이스라인',
  data: bVals,
  backgroundColor: 'rgba(74,144,217,0.2)',
  borderColor: '#4A90D9',
  borderWidth: 2,
  pointBackgroundColor: '#4A90D9',
}}];
if (hasAdv) radarDatasets.push({{
  label: '고도화 후',
  data: aVals,
  backgroundColor: 'rgba(232,112,64,0.2)',
  borderColor: '#E87040',
  borderWidth: 2,
  pointBackgroundColor: '#E87040',
}});
new Chart(radarCtx, {{
  type: 'radar',
  data: {{ labels, datasets: radarDatasets }},
  options: {{
    responsive: true, maintainAspectRatio: false,
    scales: {{ r: {{
      min: 0, max: 1,
      ticks: {{ stepSize: 0.2, color: '#666', backdropColor: 'transparent' }},
      grid: {{ color: '#2e3150' }},
      pointLabels: {{ color: '#aaa', font: {{ size: 13 }} }},
      angleLines: {{ color: '#2e3150' }},
    }} }},
    plugins: {{ legend: {{ labels: {{ color: '#aaa' }} }} }},
  }}
}});

const barCtx = document.getElementById('barChart').getContext('2d');
const barDatasets = [{{
  label: '베이스라인',
  data: bVals,
  backgroundColor: 'rgba(74,144,217,0.8)',
  borderRadius: 6,
}}];
if (hasAdv) barDatasets.push({{
  label: '고도화 후',
  data: aVals,
  backgroundColor: 'rgba(232,112,64,0.8)',
  borderRadius: 6,
}});
new Chart(barCtx, {{
  type: 'bar',
  data: {{ labels, datasets: barDatasets }},
  options: {{
    responsive: true, maintainAspectRatio: false,
    scales: {{
      y: {{ min: 0, max: 1.1, ticks: {{ color: '#666' }}, grid: {{ color: '#2e3150' }} }},
      x: {{ ticks: {{ color: '#aaa' }}, grid: {{ color: 'transparent' }} }},
    }},
    plugins: {{ legend: {{ labels: {{ color: '#aaa' }} }} }},
  }}
}});
</script>
</body>
</html>"""
    return html


def main():
    baseline = load_result(BASELINE_FILE)
    advanced = load_result(ADVANCED_FILE)

    if not baseline:
        print(f"[ERROR] {BASELINE_FILE} 파일이 없어요.")
        return

    html = generate_html(baseline, advanced)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"리포트 생성 완료: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()