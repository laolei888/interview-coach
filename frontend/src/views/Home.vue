<template>
  <main class="home-page">
    <DashboardWorkspace v-if="isLoggedIn" :user-name="userStore.username" />

    <div v-else class="landing-page">
      <section class="landing-hero">
        <div class="landing-copy">
          <div class="landing-kicker"><i></i><span>AI CAREER STUDIO</span><b>FOR TECH CANDIDATES</b></div>
          <h1>把“准备好了”<br /><em>变成有证据。</em></h1>
          <p>从简历诊断到模拟面试，再到逐题复盘，一个工作台完成一次完整的求职训练。</p>
          <div class="landing-actions"><el-button type="primary" size="large" @click="router.push('/login')">开始第一次训练<el-icon><ArrowRight /></el-icon></el-button><button type="button" @click="scrollToCapabilities"><span>01</span>先了解训练方式</button></div>
          <div class="landing-proof"><div><strong>3</strong><span>训练模块</span></div><div><strong>10/10</strong><span>逐题反馈</span></div><div><strong>1</strong><span>训练档案</span></div></div>
        </div>

        <div class="landing-stage" id="studio-preview">
          <InterviewOrbScene class="stage-orb" />
          <div class="stage-window" aria-label="训练驾驶舱预览">
            <div class="stage-bar"><span><i></i><i></i><i></i></span><small>INTERVIEW COACH / WORKSPACE</small><b>LIVE PREVIEW</b></div>
            <div class="stage-body">
              <aside class="stage-rail"><strong>IC</strong><span class="active">总览</span><span>面试</span><span>题库</span><span>复盘</span><i></i><span>设置</span></aside>
              <div class="stage-dashboard">
                <div class="stage-head"><div><small>WED · 18 SEP</small><strong>今天练什么？</strong></div><span>U</span></div>
                <div class="stage-priority"><div><small>建议优先</small><strong>完成一场技术深挖</strong><p>根据你的训练数据，下一步从回答深度开始。</p></div><b>↗</b></div>
                <div class="stage-metrics"><div><small>准备度</small><strong>78<em>/100</em></strong><span>↑ 8.6</span></div><div><small>本周训练</small><strong>06<em> 次</em></strong><span>目标 08 次</span></div></div>
                <div class="stage-insight"><div><span>回答表现</span><strong>最近 6 次回答</strong></div><div class="stage-bars"><i style="height:42%"></i><i style="height:58%"></i><i style="height:48%"></i><i style="height:72%"></i><i style="height:66%"></i><i style="height:84%"></i></div><small><span>较弱</span><span>最近一次</span></small></div>
              </div>
            </div>
          </div>
          <div class="stage-caption"><i></i>你的训练档案，会记住每一次进步</div>
        </div>
      </section>

      <section class="landing-strip"><span>一个流程，三个动作</span><div><strong>定位</strong><small>找到岗位要求</small></div><i></i><div><strong>练习</strong><small>在真实对话中验证</small></div><i></i><div><strong>复盘</strong><small>把反馈变成下一步</small></div></section>

      <section id="capabilities" class="landing-features">
        <div class="landing-section-head"><span>WHY IT WORKS</span><h2>把准备拆小，<em>把反馈做深。</em></h2><p>不再把时间花在漫无目的地刷题上。每一次训练都有上下文、有评分，也有明确的下一步。</p></div>
        <div class="landing-feature-grid"><article><div class="feature-number">01</div><el-icon><DocumentChecked /></el-icon><h3>岗位定向诊断</h3><p>结合目标岗位与 JD，识别关键词缺口、项目证据和最值得先补的能力。</p><button type="button" @click="router.push('/login')">从简历开始<el-icon><ArrowRight /></el-icon></button></article><article><div class="feature-number">02</div><el-icon><ChatDotRound /></el-icon><h3>真实对话训练</h3><p>选择面试场景和难度，接受连续追问，把“我会”变成能讲清楚的回答。</p><button type="button" @click="router.push('/login')">进入模拟面试<el-icon><ArrowRight /></el-icon></button></article><article><div class="feature-number">03</div><el-icon><DataAnalysis /></el-icon><h3>结构化复盘</h3><p>查看逐题评分、表现趋势和低分回答，把每份报告转成下一轮训练计划。</p><button type="button" @click="router.push('/login')">查看复盘方式<el-icon><ArrowRight /></el-icon></button></article></div>
      </section>

      <section id="workflow" class="landing-method"><div class="method-heading"><span>THE METHOD</span><h2>每次只推进<br /><em>最重要的一步。</em></h2></div><div class="method-list"><div><strong>01</strong><p><b>先知道自己缺什么</b><small>简历和岗位要求放在同一个上下文里，减少无效准备。</small></p><el-icon><ArrowRight /></el-icon></div><div><strong>02</strong><p><b>再练习如何讲出来</b><small>AI 面试官会根据你的回答继续追问，而不是只给固定题目。</small></p><el-icon><ArrowRight /></el-icon></div><div><strong>03</strong><p><b>最后决定下一次练什么</b><small>低分回答、薄弱题目和训练趋势都会留在你的档案里。</small></p><el-icon><ArrowRight /></el-icon></div></div></section>
      <section id="proof" class="landing-end"><div><span>START WITH ONE ANSWER</span><h2>先回答一个问题，<br /><em>再让数据告诉你下一步。</em></h2></div><el-button type="primary" size="large" @click="router.push('/login')">建立我的训练档案<el-icon><ArrowRight /></el-icon></el-button></section>
    </div>
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatDotRound, DataAnalysis, DocumentChecked } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import DashboardWorkspace from '../components/DashboardWorkspace.vue'
import InterviewOrbScene from '../components/InterviewOrbScene.vue'

const router = useRouter()
const userStore = useUserStore()
// 登录状态需要保持响应式：用户登录/退出后，首页应立即在访客落地页与训练工作台之间切换。
const isLoggedIn = computed(() => userStore.isLoggedIn())
const scrollToCapabilities = () => document.querySelector('#capabilities')?.scrollIntoView({ behavior: 'smooth' })
</script>

<style scoped>
.home-page { max-width: 1280px; margin: 0 auto; color: var(--text); }
.landing-page { max-width: 1220px; margin: 0 auto; padding: 0 28px 72px; }.landing-hero { min-height: 650px; display: grid; grid-template-columns: minmax(0,.82fr) minmax(500px,1.18fr); align-items: center; gap: clamp(38px,6vw,88px); padding: 72px 0 58px; }.landing-copy { position: relative; z-index: 2; }.landing-kicker { display: flex; align-items: center; gap: 9px; color: var(--blue); font: 800 10px ui-monospace,monospace; letter-spacing: .08em; }.landing-kicker i { width: 7px; height: 7px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 5px color-mix(in srgb, var(--accent) 15%, transparent); }.landing-kicker b { margin-left: 4px; color: var(--text-muted); font-size: 9px; font-weight: 500; letter-spacing: .03em; }.landing-copy h1 { max-width: 610px; margin: 22px 0 20px; color: var(--text); font-size: clamp(50px,6vw,78px); line-height: 1.03; letter-spacing: -.075em; }.landing-copy h1 em { color: var(--blue); font-style: normal; }.landing-copy > p { max-width: 460px; color: var(--text-secondary); font-size: 15px; line-height: 1.9; }.landing-actions { display: flex; align-items: center; gap: 22px; margin-top: 32px; }.landing-actions .el-button { height: 46px; padding-inline: 20px; border-radius: 7px; font-weight: 700; }.landing-actions > button { display: flex; align-items: center; gap: 8px; border: 0; background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 12px; }.landing-actions > button span { display: grid; place-items: center; width: 24px; height: 24px; border: 1px solid var(--border); border-radius: 50%; color: var(--blue); font: 700 9px ui-monospace,monospace; }.landing-proof { display: flex; gap: 28px; margin-top: 52px; }.landing-proof > div { padding-left: 12px; border-left: 2px solid var(--border); }.landing-proof strong,.landing-proof span { display: block; }.landing-proof strong { color: var(--text); font: 700 20px ui-monospace,monospace; }.landing-proof span { margin-top: 4px; color: var(--text-muted); font-size: 10px; }
.landing-stage { min-height: 500px; display: grid; place-items: center; position: relative; }.landing-stage::before { content: ''; position: absolute; width: 80%; height: 70%; border-radius: 50%; background: radial-gradient(circle, color-mix(in srgb, var(--blue) 17%, transparent), transparent 68%); filter: blur(18px); }.stage-orb { inset: 4% -8% 0 20%; opacity: .72; }.stage-window { width: min(100%,590px); position: relative; z-index: 1; overflow: hidden; border: 1px solid color-mix(in srgb, var(--blue) 28%, var(--border)); border-radius: 10px; background: var(--bg-card); box-shadow: 0 30px 70px rgba(23,32,42,.17); transform: rotate(1.4deg); }.stage-bar { display: flex; align-items: center; justify-content: space-between; height: 38px; padding: 0 14px; border-bottom: 1px solid var(--border); background: color-mix(in srgb, var(--bg-card) 86%, var(--blue-light)); }.stage-bar > span { display: flex; gap: 5px; }.stage-bar i { width: 7px; height: 7px; border-radius: 50%; background: var(--border); }.stage-bar i:first-child { background: #df694f; }.stage-bar i:nth-child(2) { background: #d6a44d; }.stage-bar i:nth-child(3) { background: #58b98b; }.stage-bar small { color: var(--text-muted); font: 8px ui-monospace,monospace; letter-spacing: .08em; }.stage-bar b { color: var(--success); font: 700 8px ui-monospace,monospace; letter-spacing: .04em; }.stage-body { display: grid; grid-template-columns: 58px 1fr; min-height: 380px; }.stage-rail { display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 18px 8px; border-right: 1px solid var(--border); background: color-mix(in srgb, var(--bg-soft) 76%, transparent); }.stage-rail strong { display: grid; place-items: center; width: 27px; height: 27px; margin-bottom: 17px; border-radius: 7px; background: var(--blue); color: #fff; font: 800 10px ui-monospace,monospace; }.stage-rail span { width: 100%; padding: 7px 0; border-radius: 5px; color: var(--text-muted); font-size: 8px; text-align: center; }.stage-rail span.active { background: var(--blue-light); color: var(--blue); font-weight: 700; }.stage-rail i { flex: 1; }.stage-dashboard { padding: 23px 25px; background: linear-gradient(145deg,color-mix(in srgb,var(--blue-light) 20%,var(--bg-card)),var(--bg-card)); }.stage-head { display: flex; align-items: center; justify-content: space-between; }.stage-head small,.stage-head strong { display: block; }.stage-head small { color: var(--text-muted); font: 8px ui-monospace,monospace; letter-spacing: .1em; }.stage-head strong { margin-top: 7px; color: var(--text); font-size: 18px; }.stage-head > span { display: grid; place-items: center; width: 29px; height: 29px; border-radius: 50%; background: var(--text); color: var(--bg-card); font: 700 10px ui-monospace,monospace; }.stage-priority { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-top: 22px; padding: 18px; border: 1px solid color-mix(in srgb,var(--blue) 20%,var(--border)); border-radius: 8px; background: var(--blue-light); }.stage-priority small { color: var(--blue); font-size: 9px; font-weight: 700; }.stage-priority strong { display: block; margin-top: 7px; color: var(--text); font-size: 14px; }.stage-priority p { margin-top: 5px; color: var(--text-secondary); font-size: 10px; line-height: 1.55; }.stage-priority > b { color: var(--blue); font-size: 20px; font-weight: 400; }.stage-metrics { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 12px; }.stage-metrics > div { padding: 14px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); }.stage-metrics small,.stage-metrics strong,.stage-metrics span { display: block; }.stage-metrics small { color: var(--text-muted); font-size: 9px; }.stage-metrics strong { margin: 6px 0 3px; color: var(--text); font: 700 24px ui-monospace,monospace; }.stage-metrics strong em { color: var(--text-muted); font-size: 10px; font-style: normal; }.stage-metrics span { color: var(--success); font: 700 9px ui-monospace,monospace; }.stage-insight { margin-top: 12px; padding: 15px; border: 1px solid var(--border); border-radius: 7px; background: var(--bg-card); }.stage-insight > div:first-child { display: flex; justify-content: space-between; }.stage-insight > div:first-child span { color: var(--text-muted); font-size: 9px; }.stage-insight > div:first-child strong { color: var(--text-secondary); font-size: 10px; }.stage-bars { display: flex; align-items: end; gap: 12px; height: 105px; margin-top: 14px; padding: 0 5px; border-bottom: 1px solid var(--border); }.stage-bars i { flex: 1; min-height: 10px; border-radius: 4px 4px 0 0; background: linear-gradient(180deg,var(--blue),color-mix(in srgb,var(--blue) 42%,transparent)); }.stage-bars i:nth-last-child(-n+2) { background: linear-gradient(180deg,var(--accent),color-mix(in srgb,var(--accent) 46%,transparent)); }.stage-insight > small { display: flex; justify-content: space-between; margin-top: 7px; color: var(--text-muted); font-size: 8px; }.stage-caption { display: flex; align-items: center; justify-content: center; gap: 6px; position: absolute; right: 0; bottom: 5px; color: var(--text-muted); font-size: 9px; }.stage-caption i { width: 5px; height: 5px; border-radius: 50%; background: var(--accent); }
.landing-strip { display: grid; grid-template-columns: 1.2fr 1fr 24px 1fr 24px 1fr; align-items: center; gap: 16px; padding: 18px 24px; border: 1px solid var(--border); border-radius: 8px; background: var(--surface-glass); }.landing-strip > span { color: var(--text-muted); font: 700 9px ui-monospace,monospace; letter-spacing: .08em; }.landing-strip > div strong,.landing-strip > div small { display: block; }.landing-strip > div strong { color: var(--text); font-size: 12px; }.landing-strip > div small { margin-top: 4px; color: var(--text-muted); font-size: 9px; }.landing-strip > i { width: 24px; height: 1px; background: var(--border); }
.landing-features { padding: 112px 0 122px; }.landing-section-head { display: grid; grid-template-columns: .8fr 1fr 1fr; align-items: end; gap: 30px; margin-bottom: 38px; }.landing-section-head > span,.method-heading > span,.landing-end > div > span { color: var(--blue); font: 800 10px ui-monospace,monospace; letter-spacing: .08em; }.landing-section-head h2,.method-heading h2,.landing-end h2 { color: var(--text); font-size: clamp(30px,4vw,50px); line-height: 1.08; letter-spacing: -.05em; }.landing-section-head h2 em,.method-heading h2 em,.landing-end h2 em { color: var(--blue); font-style: normal; }.landing-section-head p { max-width: 330px; justify-self: end; color: var(--text-secondary); font-size: 13px; line-height: 1.8; }.landing-feature-grid { display: grid; grid-template-columns: repeat(3,1fr); gap: 12px; }.landing-feature-grid article { min-height: 270px; padding: 24px; border-top: 2px solid var(--border); transition: border-color .2s, transform .2s; }.landing-feature-grid article:hover { border-top-color: var(--blue); transform: translateY(-4px); }.feature-number { color: var(--text-muted); font: 700 10px ui-monospace,monospace; }.landing-feature-grid article > .el-icon { width: 40px; height: 40px; display: grid; place-items: center; margin: 25px 0 20px; border-radius: 8px; background: var(--blue-light); color: var(--blue); font-size: 20px; }.landing-feature-grid h3 { margin-bottom: 9px; color: var(--text); font-size: 17px; }.landing-feature-grid p { min-height: 67px; color: var(--text-secondary); font-size: 12px; line-height: 1.8; }.landing-feature-grid button { display: flex; align-items: center; gap: 5px; margin-top: 20px; border: 0; background: transparent; color: var(--blue); cursor: pointer; font-size: 11px; font-weight: 700; }
.landing-method { display: grid; grid-template-columns: .8fr 1.2fr; gap: 70px; padding: 78px 54px; border-radius: 10px; background: #172724; color: #fff; }.landing-method .method-heading h2 { margin-top: 13px; color: #fff; }.landing-method .method-heading h2 em { color: #8ed3ca; }.method-list { border-top: 1px solid rgba(255,255,255,.15); }.method-list > div { display: grid; grid-template-columns: 42px 1fr 20px; align-items: center; gap: 16px; min-height: 94px; border-bottom: 1px solid rgba(255,255,255,.15); }.method-list > div > strong { color: #8ed3ca; font: 700 11px ui-monospace,monospace; }.method-list p b,.method-list p small { display: block; }.method-list p b { color: #fff; font-size: 14px; }.method-list p small { margin-top: 5px; color: rgba(255,255,255,.55); font-size: 11px; line-height: 1.5; }.method-list .el-icon { color: #8ed3ca; }.landing-end { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-top: 22px; padding: 30px 34px; border-radius: 9px; background: var(--blue-light); }.landing-end h2 { margin-top: 9px; font-size: clamp(25px,3vw,36px); }.landing-end .el-button { flex-shrink: 0; }
@media (max-width: 1050px) { .landing-hero { grid-template-columns: 1fr; gap: 10px; padding-top: 55px; }.landing-copy { max-width: 650px; }.landing-stage { min-height: 500px; margin-top: 18px; }.landing-section-head { grid-template-columns: 1fr 1fr; }.landing-section-head p { grid-column: 2; }.landing-method { gap: 40px; } }
@media (max-width: 760px) { .home-page { width: 100%; }.landing-page { padding: 0 16px 46px; }.landing-hero { min-height: 0; padding: 42px 0 28px; }.landing-copy h1 { margin-top: 18px; font-size: clamp(43px,12vw,64px); }.landing-copy > p { font-size: 14px; }.landing-kicker b { display: none; }.landing-actions { align-items: flex-start; flex-direction: column; gap: 16px; margin-top: 25px; }.landing-proof { gap: 18px; margin-top: 34px; }.landing-stage { min-height: 390px; margin: 12px -4px 0; }.stage-orb { display: none; }.stage-window { width: 100%; transform: none; }.stage-body { min-height: 330px; }.stage-dashboard { padding: 19px 15px; }.stage-priority { padding: 14px; }.stage-caption { right: 0; bottom: -2px; }.landing-strip { grid-template-columns: repeat(3,1fr); gap: 9px; padding: 16px; }.landing-strip > span { grid-column: 1 / -1; margin-bottom: 4px; }.landing-strip > i { display: none; }.landing-features { padding: 74px 0 76px; }.landing-section-head { display: block; }.landing-section-head h2 { margin: 13px 0 16px; font-size: 36px; }.landing-section-head p { max-width: 430px; }.landing-feature-grid { grid-template-columns: 1fr; gap: 26px; margin-top: 30px; }.landing-feature-grid article { min-height: 0; padding: 20px 0 0; }.landing-feature-grid article > .el-icon { margin: 19px 0 15px; }.landing-feature-grid p { min-height: 0; }.landing-method { display: block; padding: 48px 22px; }.method-list { margin-top: 35px; }.method-list > div { min-height: 104px; }.landing-end { align-items: flex-start; flex-direction: column; padding: 25px 22px; }.landing-end .el-button { width: 100%; } }
@media (max-width: 430px) { .landing-proof { gap: 12px; }.landing-proof > div { padding-left: 8px; }.landing-proof strong { font-size: 17px; }.landing-proof span { font-size: 9px; }.stage-rail { padding-inline: 5px; }.stage-rail span { font-size: 7px; }.stage-head strong { font-size: 16px; }.stage-metrics strong { font-size: 21px; }.stage-bars { gap: 8px; } }
/* ======== Editorial premium layer ======== */
.landing-page { position: relative; }
.landing-hero { position: relative; isolation: isolate; }
.landing-hero::before {
  content: '';
  position: absolute;
  inset: 34px -42px 16px;
  z-index: -1;
  border: 1px solid color-mix(in srgb, var(--blue) 15%, transparent);
  border-radius: 34px;
  background:
    radial-gradient(circle at 79% 22%, color-mix(in srgb, var(--blue) 15%, transparent), transparent 28%),
    linear-gradient(135deg, color-mix(in srgb, var(--blue-light) 30%, transparent), transparent 54%);
  pointer-events: none;
}
.landing-copy h1 { text-shadow: 0 16px 36px color-mix(in srgb, var(--blue) 11%, transparent); }
.landing-actions .el-button { border-radius: 12px; box-shadow: 0 15px 32px color-mix(in srgb, var(--blue) 24%, transparent); }
.landing-actions .el-button:hover { box-shadow: 0 19px 38px color-mix(in srgb, var(--blue) 32%, transparent); }
.landing-stage { min-height: 540px; }
.landing-stage::before { width: 92%; height: 78%; filter: blur(28px); opacity: .9; }
.stage-window {
  border-radius: 20px;
  border-color: color-mix(in srgb, var(--blue) 32%, var(--border));
  box-shadow: 0 42px 100px rgba(16, 54, 47, .2), 0 0 0 8px color-mix(in srgb, var(--blue) 5%, transparent);
  transform: rotate(1.1deg) translateY(-5px);
}
.stage-bar { height: 44px; padding-inline: 18px; background: color-mix(in srgb, var(--bg-card) 76%, var(--blue-light)); }
.stage-bar i { width: 8px; height: 8px; }
.stage-body { min-height: 410px; }
.stage-rail { padding: 22px 10px; background: color-mix(in srgb, var(--bg-soft) 68%, transparent); }
.stage-rail strong { width: 31px; height: 31px; border-radius: 10px; box-shadow: 0 8px 18px color-mix(in srgb, var(--blue) 22%, transparent); }
.stage-rail span, .stage-metrics > div, .stage-insight, .stage-priority { border-radius: 12px; }
.stage-dashboard { padding: 28px 29px; }
.stage-priority { box-shadow: 0 12px 24px color-mix(in srgb, var(--blue) 9%, transparent); }
.stage-metrics > div, .stage-insight { box-shadow: 0 10px 24px rgba(16, 54, 47, .045); }
.stage-caption { bottom: -2px; padding: 7px 11px; border: 1px solid var(--border); border-radius: 999px; background: var(--surface-glass); box-shadow: 0 10px 24px rgba(16, 54, 47, .06); backdrop-filter: blur(12px); }
.landing-strip {
  border-radius: 16px;
  border-color: color-mix(in srgb, var(--blue) 15%, var(--border));
  box-shadow: 0 22px 56px rgba(16, 54, 47, .07);
  backdrop-filter: blur(18px);
}
.landing-strip > div { padding: 5px 0; }
.landing-strip > i { background: color-mix(in srgb, var(--blue) 42%, var(--border)); }
.landing-features { position: relative; }
.landing-feature-grid { gap: 16px; }
.landing-feature-grid article {
  min-height: 292px;
  border: 1px solid var(--border);
  border-top: 2px solid color-mix(in srgb, var(--blue) 42%, var(--border));
  border-radius: 16px;
  background: var(--surface-glass);
  box-shadow: 0 18px 46px rgba(16, 54, 47, .055);
  backdrop-filter: blur(16px);
  transition: border-color .22s, transform .22s, box-shadow .22s;
}
.landing-feature-grid article:hover {
  border-top-color: var(--blue);
  border-color: color-mix(in srgb, var(--blue) 32%, var(--border));
  box-shadow: 0 26px 60px color-mix(in srgb, var(--blue) 12%, transparent);
  transform: translateY(-7px);
}
.landing-feature-grid article > .el-icon { border-radius: 12px; box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--blue) 14%, transparent); }
.landing-method {
  position: relative;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, #0c211c 0%, #163a32 58%, #1d5147 100%);
  box-shadow: 0 32px 82px rgba(10, 43, 35, .22);
}
.landing-method::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  right: -150px;
  top: -180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(112, 211, 194, .28), transparent 66%);
  pointer-events: none;
}
.landing-method .method-heading, .landing-method .method-list { position: relative; z-index: 1; }
.method-list > div { transition: padding .2s, background .2s; }
.method-list > div:hover { padding-inline: 10px; background: rgba(255, 255, 255, .045); }
.landing-end {
  position: relative;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--blue) 17%, var(--border));
  border-radius: 18px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--blue-light) 82%, var(--bg-card)), var(--bg-card));
  box-shadow: 0 22px 58px rgba(16, 54, 47, .08);
}
.landing-end::after { content: ''; position: absolute; width: 260px; height: 260px; right: -115px; bottom: -145px; border-radius: 50%; background: radial-gradient(circle, color-mix(in srgb, var(--blue) 20%, transparent), transparent 68%); pointer-events: none; }
.landing-end > div, .landing-end > .el-button { position: relative; z-index: 1; }
@media (max-width: 760px) {
  .landing-hero::before { inset: 18px -18px 6px; border-radius: 24px; }
  .landing-stage { min-height: 420px; }
  .stage-window { transform: none; box-shadow: 0 28px 64px rgba(16, 54, 47, .16); }
  .stage-body { min-height: 350px; }
  .stage-dashboard { padding: 21px 16px; }
  .landing-feature-grid article { min-height: 0; border-radius: 14px; background: var(--surface-glass); }
  .landing-method { border-radius: 18px; }
}
.landing-actions > .el-button--primary { color: #fff !important; }
.landing-actions > .el-button--primary > span { display: inline-flex; place-items: initial; width: auto; height: auto; border: 0; border-radius: 0; background: transparent; color: inherit; font: inherit; }

/* ======== Final composition pass ========
   Keep the landing page airy on desktop, but make the preview a compact,
   readable product moment on narrow screens instead of a second full page. */
.landing-page { max-width: 1200px; padding-inline: clamp(20px, 3vw, 30px); }
.landing-hero {
  min-height: min(720px, calc(100vh - 56px));
  grid-template-columns: minmax(360px, .88fr) minmax(520px, 1.12fr);
  gap: clamp(42px, 5vw, 72px);
  padding: 78px 0 66px;
}
.landing-copy { max-width: 620px; }
.landing-copy h1 { max-width: 600px; }
.landing-proof {
  gap: 0;
  padding-top: 20px;
  border-top: 1px solid color-mix(in srgb, var(--border) 70%, transparent);
}
.landing-proof > div { flex: 0 1 112px; }
.landing-stage { min-height: 520px; }
.stage-window { width: min(100%, 620px); }
.landing-strip { margin-top: 2px; }

@media (max-width: 1050px) {
  .landing-hero { min-height: 0; grid-template-columns: 1fr; gap: 12px; padding-top: 58px; }
  .landing-copy { max-width: 680px; }
  .landing-stage { min-height: 500px; margin-top: 12px; }
}

@media (max-width: 760px) {
  .landing-page { width: 100%; max-width: none; padding: 0 16px 52px; }
  .landing-hero { min-height: 0; gap: 0; padding: 43px 0 22px; }
  .landing-hero::before { inset: 18px -12px 0; border-radius: 24px; }
  .landing-copy h1 { max-width: 100%; margin: 17px 0 17px; font-size: clamp(43px, 11.6vw, 58px); line-height: .99; }
  .landing-copy > p { max-width: 390px; line-height: 1.75; }
  .landing-actions { margin-top: 24px; }
  .landing-proof { margin-top: 30px; padding-top: 17px; }
  .landing-proof > div { flex: 1 1 0; min-width: 0; }
  .landing-proof strong { font-size: 18px; }
  .landing-proof span { white-space: nowrap; }
  .landing-stage {
    min-height: 0;
    height: auto;
    margin: 20px -2px 0;
    align-items: start;
  }
  .stage-window { width: 100%; transform: none; box-shadow: 0 24px 56px rgba(16, 54, 47, .14); }
  .stage-bar { height: 40px; padding-inline: 13px; }
  .stage-bar small { font-size: 7px; }
  .stage-body { min-height: 0; }
  .stage-dashboard { padding: 17px 13px 15px; }
  .stage-head strong { font-size: 16px; }
  .stage-priority { gap: 10px; margin-top: 16px; padding: 12px 13px; }
  .stage-priority strong { margin-top: 5px; font-size: 13px; }
  .stage-priority p { max-width: 245px; font-size: 9px; line-height: 1.45; }
  .stage-metrics { gap: 8px; margin-top: 10px; }
  .stage-metrics > div { padding: 11px; }
  .stage-metrics strong { margin: 4px 0 2px; font-size: 20px; }
  .stage-insight { margin-top: 10px; padding: 11px; }
  .stage-bars { height: 60px; gap: 7px; margin-top: 9px; }
  .stage-caption { right: 50%; bottom: -2px; transform: translateX(50%); white-space: nowrap; font-size: 8px; }
  .landing-strip { margin-top: 3px; }
  .landing-features { padding-top: 68px; }
}

@media (max-width: 430px) {
  .landing-copy h1 { font-size: 46px; }
  .landing-proof strong { font-size: 17px; }
  .stage-rail { width: 54px; }
  .stage-body { grid-template-columns: 54px 1fr; }
  .stage-dashboard { padding-inline: 11px; }
  .stage-priority p { max-width: 210px; }
}
</style>
