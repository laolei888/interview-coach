<template>
  <div
    ref="stageRef"
    :class="[
      'virtual-interviewer',
      `state-${state}`,
      { 'is-ready': stageReady, 'is-low-power': lowPower, 'has-failed': loadFailed }
    ]"
    :aria-label="`动漫虚拟面试官，当前状态：${stateLabel}`"
    role="img"
  >
    <canvas ref="canvasRef"></canvas>
    <div class="scene-atmosphere" aria-hidden="true"><i></i><i></i><i></i></div>
    <div class="scene-vignette" aria-hidden="true"></div>

    <div v-if="modelLoading && !loadFailed" class="model-loader" aria-live="polite">
      <div class="loader-portrait">
        <img src="/models/interviewer-preview.png" alt="" />
        <span><i></i><i></i><i></i></span>
      </div>
      <strong>正在进入动漫面试间</strong>
      <small>加载角色、表情与自然动作</small>
      <div class="loader-track"><i :style="{ width: `${Math.max(8, modelProgress)}%` }"></i></div>
      <em>{{ modelProgress ? `${modelProgress}%` : '准备中' }}</em>
    </div>

    <div v-if="loadFailed" class="avatar-fallback">
      <div class="fallback-halo"></div>
      <img src="/models/interviewer-preview.png" alt="动漫面试官头像" />
      <strong>林屿 · AI 面试官</strong>
      <small>当前设备暂时无法加载 3D 角色，已切换为头像模式</small>
    </div>

    <div v-if="stageReady" class="avatar-signature" aria-hidden="true"><i></i><span>VRM LIVE</span></div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  state: {
    type: String,
    default: 'idle',
    validator: value => ['preparing', 'thinking', 'speaking', 'listening', 'idle', 'ended', 'error'].includes(value)
  },
  performanceMode: {
    type: String,
    default: 'auto',
    validator: value => ['auto', 'quality', 'reduced'].includes(value)
  },
  speechText: { type: String, default: '' },
  speechProgress: { type: Number, default: 0 }
})

const emit = defineEmits(['ready', 'fallback'])
const stageRef = ref(null)
const canvasRef = ref(null)
const stageReady = ref(false)
const modelLoading = ref(true)
const modelProgress = ref(0)
const loadFailed = ref(false)
const lowPower = ref(false)
const stateLabel = computed(() => ({
  preparing: '正在准备', thinking: '正在思考', speaking: '正在提问', listening: '正在倾听',
  idle: '等待回答', ended: '面试结束', error: '连接异常'
}[props.state] || '等待回答'))

const MODEL_URL = '/models/interviewer-anime.vrm'
const IDLE_ANIMATION_URL = '/models/interviewer-idle.vrma'
const MODEL_BYTES = 21047556

let THREE
let VRMUtils
let renderer
let scene
let camera
let avatarVrm
let animationMixer
let lookAtTarget
let resizeObserver
let frameId = 0
let startedAt = 0
let lastFrameAt = 0
let nextBlinkAt = 0
let blinkStartedAt = 0
let pointerX = 0
let pointerY = 0
let targetFocusY = 1.34
let baseAvatarY = 0
let baseAvatarRotationY = 0
const disposables = []
const bones = {}

const createMaterial = (color, options = {}) => {
  const value = new THREE.MeshStandardMaterial({ color, roughness: .72, metalness: .03, ...options })
  disposables.push(value)
  return value
}

const addMesh = (geometry, meshMaterial, parent = scene) => {
  disposables.push(geometry)
  const mesh = new THREE.Mesh(geometry, meshMaterial)
  mesh.receiveShadow = !lowPower.value
  parent.add(mesh)
  return mesh
}

const makeBox = (size, color, position, parent = scene, options = {}) => {
  const mesh = addMesh(new THREE.BoxGeometry(...size), createMaterial(color, options), parent)
  mesh.position.set(...position)
  return mesh
}

function createRoom() {
  const wall = makeBox([9.2, 5.3, .12], 0x171a2d, [0, 2.15, -2.34])
  wall.receiveShadow = true
  const floor = makeBox([9.2, .1, 7.5], 0x0d1020, [0, -.04, .38], scene, { roughness: .88 })
  floor.receiveShadow = true

  const panel = addMesh(new THREE.PlaneGeometry(2.75, 1.66), createMaterial(0x263057, { emissive: 0x263d6b, emissiveIntensity: .32, roughness: .45 }))
  panel.position.set(2.45, 2.65, -2.26)
  const panelGlow = addMesh(new THREE.PlaneGeometry(2.35, 1.28), createMaterial(0x8eafd0, { emissive: 0x466b93, emissiveIntensity: .72, roughness: .28 }))
  panelGlow.position.set(2.45, 2.65, -2.19)

  for (let index = 0; index < 7; index += 1) {
    makeBox([.035, 2.55, .045], index % 2 ? 0x313a5a : 0x222a48, [-3.5 + index * .22, 2.26, -2.24], scene, { roughness: .9 })
  }
  makeBox([1.52, .06, .055], 0xb39475, [-2.78, 1.44, -2.16], scene, { emissive: 0x5b3c2c, emissiveIntensity: .12 })

  const desk = makeBox([5.2, .16, 1.55], 0x5b4034, [0, .57, .65], scene, { roughness: .63 })
  desk.receiveShadow = true
  makeBox([5.12, .035, 1.49], 0x8d6650, [0, .665, .65], scene, { roughness: .55 })
  makeBox([.18, 1.15, 1.16], 0x292336, [-2.05, .02, .65])
  makeBox([.18, 1.15, 1.16], 0x292336, [2.05, .02, .65])

  const paper = makeBox([.72, .018, .52], 0xf0e9df, [.92, .68, .4], scene, { roughness: 1 })
  paper.rotation.y = -.08
  const tablet = makeBox([.62, .025, .42], 0x20293d, [-.98, .69, .47], scene, { emissive: 0x263c63, emissiveIntensity: .25 })
  tablet.rotation.y = .1

  const lampStem = addMesh(new THREE.CylinderGeometry(.022, .022, .78, 16), createMaterial(0xc79a6e))
  lampStem.position.set(-2.72, 1.1, -1.58)
  const lampShade = addMesh(new THREE.ConeGeometry(.3, .42, 28, 1, true), createMaterial(0xd5b08a, { side: THREE.DoubleSide, emissive: 0x6f492f, emissiveIntensity: .35 }))
  lampShade.position.set(-2.72, 1.53, -1.58)
  const lampGlow = new THREE.PointLight(0xffba83, 7, 3.4)
  lampGlow.position.set(-2.72, 1.35, -1.35)
  scene.add(lampGlow)

  const pot = addMesh(new THREE.CylinderGeometry(.17, .22, .34, 24), createMaterial(0xa66d58))
  pot.position.set(2.93, .2, -1.7)
  for (let index = 0; index < 7; index += 1) {
    const leaf = addMesh(new THREE.SphereGeometry(.17, 18, 14), createMaterial(index % 2 ? 0x417967 : 0x568f77))
    leaf.position.set(2.93 + Math.sin(index * 1.7) * .2, .53 + (index % 3) * .16, -1.7 + Math.cos(index * 1.3) * .09)
    leaf.scale.set(.48, 1.38, .34)
    leaf.rotation.z = (index - 3) * .16
  }
}

function setupLighting() {
  scene.add(new THREE.HemisphereLight(0xcad9ff, 0x18152b, lowPower.value ? 1.65 : 2.05))
  const key = new THREE.DirectionalLight(0xffe7d6, lowPower.value ? 2.35 : 3.15)
  key.position.set(-2.7, 4.5, 3.4)
  scene.add(key)
  const fill = new THREE.DirectionalLight(0x91b7ff, 1.75)
  fill.position.set(3.4, 2.8, 1.6)
  scene.add(fill)
  const rim = new THREE.PointLight(0x9a82ff, 9, 5.5)
  rim.position.set(1.65, 2.42, -.45)
  scene.add(rim)
}

function loadWithProgress(loader, url) {
  return new Promise((resolve, reject) => {
    loader.load(url, resolve, event => {
      const total = event.total || MODEL_BYTES
      modelProgress.value = Math.min(96, Math.max(modelProgress.value, Math.round(event.loaded / total * 100)))
    }, reject)
  })
}

async function loadAvatar(GLTFLoader, VRMLoaderPlugin) {
  const loader = new GLTFLoader()
  loader.register(parser => new VRMLoaderPlugin(parser))
  const gltf = await loadWithProgress(loader, MODEL_URL)
  const vrm = gltf.userData.vrm
  if (!vrm) throw new Error('模型中未找到 VRM 数据')

  VRMUtils.rotateVRM0(vrm)
  vrm.scene.name = 'AnimeInterviewer'
  vrm.scene.traverse(object => {
    object.frustumCulled = false
    if (object.isMesh) {
      object.castShadow = false
      object.receiveShadow = false
    }
  })
  vrm.scene.position.set(0, .015, -.36)
  vrm.scene.scale.setScalar(1.06)
  scene.add(vrm.scene)
  avatarVrm = vrm
  baseAvatarY = vrm.scene.position.y
  baseAvatarRotationY = vrm.scene.rotation.y

  const modelBox = new THREE.Box3().setFromObject(vrm.scene)
  const modelSize = modelBox.getSize(new THREE.Vector3())
  const modelCenter = modelBox.getCenter(new THREE.Vector3())
  targetFocusY = modelBox.min.y + modelSize.y * .69
  camera.position.set(.02, targetFocusY + .03, 2.5)
  camera.lookAt(modelCenter.x, targetFocusY, -.28)

  lookAtTarget = new THREE.Object3D()
  lookAtTarget.position.set(0, targetFocusY + .035, 2.4)
  scene.add(lookAtTarget)
  if (vrm.lookAt) vrm.lookAt.target = lookAtTarget

  ;['head', 'neck', 'chest', 'upperChest', 'spine', 'leftUpperArm', 'rightUpperArm', 'leftHand', 'rightHand'].forEach(name => {
    bones[name] = vrm.humanoid?.getNormalizedBoneNode(name)
  })
  vrm.expressionManager?.setValue('neutral', 1)
  vrm.expressionManager?.setValue('relaxed', .06)
  modelProgress.value = 98
}

async function loadIdleAnimation(GLTFLoader, VRMAnimationLoaderPlugin, createVRMAnimationClip, VRMLookAtQuaternionProxy) {
  if (!avatarVrm) return false
  try {
    const loader = new GLTFLoader()
    loader.register(parser => new VRMAnimationLoaderPlugin(parser))
    const gltf = await loader.loadAsync(IDLE_ANIMATION_URL)
    const animation = gltf.userData.vrmAnimations?.[0]
    if (!animation) return false
    if (avatarVrm.lookAt && VRMLookAtQuaternionProxy) {
      const lookAtProxy = new VRMLookAtQuaternionProxy(avatarVrm.lookAt)
      lookAtProxy.name = 'VRMLookAtQuaternionProxy'
      avatarVrm.scene.add(lookAtProxy)
    }
    const clip = createVRMAnimationClip(animation, avatarVrm)
    animationMixer = new THREE.AnimationMixer(avatarVrm.scene)
    const action = animationMixer.clipAction(clip)
    action.setLoop(THREE.LoopRepeat, Infinity)
    action.clampWhenFinished = false
    action.play()
    return true
  } catch (error) {
    console.warn('VRM idle animation unavailable', error)
    return false
  }
}

function resize() {
  if (!renderer || !camera || !stageRef.value) return
  const rect = stageRef.value.getBoundingClientRect()
  const width = Math.max(1, rect.width)
  const height = Math.max(1, rect.height)
  renderer.setSize(width, height, false)
  camera.aspect = width / height
  camera.updateProjectionMatrix()
}

function scheduleBlink(now) {
  nextBlinkAt = now + 2400 + Math.random() * 3300
  blinkStartedAt = 0
}

function blinkWeight(now) {
  if (now >= nextBlinkAt && !blinkStartedAt) blinkStartedAt = now
  if (!blinkStartedAt) return 0
  const elapsed = now - blinkStartedAt
  if (elapsed > 210) {
    scheduleBlink(now)
    return 0
  }
  return elapsed < 82 ? Math.min(1, elapsed / 82) : Math.max(0, 1 - (elapsed - 82) / 128)
}

function updateExpressions(now, elapsed) {
  const manager = avatarVrm?.expressionManager
  if (!manager) return
  const speaking = props.state === 'speaking'
  const listening = props.state === 'listening'
  const thinking = props.state === 'thinking' || props.state === 'preparing'
  const ended = props.state === 'ended'
  const error = props.state === 'error'

  manager.setValue('blink', blinkWeight(now))
  manager.setValue('happy', ended ? .34 : speaking ? .08 : 0)
  manager.setValue('relaxed', listening ? .12 : thinking ? .09 : speaking ? .05 : .075)
  manager.setValue('sad', error ? .18 : 0)

  const mouthNames = ['aa', 'ih', 'ou', 'ee', 'oh']
  mouthNames.forEach(name => manager.setValue(name, 0))
  if (speaking) {
    const text = props.speechText || ''
    const progressIndex = Math.min(Math.max(0, text.length - 1), Math.floor(text.length * Math.max(0, Math.min(1, props.speechProgress))))
    const characterSeed = text.charCodeAt(progressIndex) || 0
    const cadence = Math.abs(Math.sin(elapsed * 10.8) * .68 + Math.sin(elapsed * 17.1) * .24)
    const primaryIndex = Math.abs(Math.floor(elapsed * 7.6 + characterSeed)) % mouthNames.length
    manager.setValue(mouthNames[primaryIndex], .14 + cadence * .54)
    manager.setValue(mouthNames[(primaryIndex + 1) % mouthNames.length], cadence * .08)
  }
}

function applyStateMotion(elapsed) {
  if (!avatarVrm) return
  const speaking = props.state === 'speaking'
  const listening = props.state === 'listening'
  const thinking = props.state === 'thinking' || props.state === 'preparing'
  if (animationMixer) animationMixer.timeScale = speaking ? 1.1 : listening ? .88 : thinking ? .72 : .8

  avatarVrm.scene.position.y = baseAvatarY + Math.sin(elapsed * 1.28) * .0035
  const targetTurn = thinking ? .035 : speaking ? Math.sin(elapsed * .78) * .012 : 0
  avatarVrm.scene.rotation.y += (baseAvatarRotationY + targetTurn - avatarVrm.scene.rotation.y) * .07

  if (bones.head) {
    const nod = listening ? Math.max(0, Math.sin(elapsed * .82)) * .018 : speaking ? Math.sin(elapsed * 1.18) * .009 : thinking ? -.018 : 0
    bones.head.quaternion.multiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(nod, 0, speaking ? Math.sin(elapsed * .55) * .007 : 0)))
  }
  if (speaking) {
    const gesture = .018 + Math.max(0, Math.sin(elapsed * 1.42)) * .026
    bones.rightUpperArm?.quaternion.multiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(-gesture, 0, gesture * .7)))
    bones.leftUpperArm?.quaternion.multiply(new THREE.Quaternion().setFromEuler(new THREE.Euler(-gesture * .45, 0, -gesture * .35)))
  }
}

function updateLookAt(elapsed) {
  if (!lookAtTarget) return
  const thinking = props.state === 'thinking' || props.state === 'preparing'
  const listening = props.state === 'listening'
  const targetX = thinking ? -.14 + Math.sin(elapsed * .42) * .08 : pointerX * (listening ? .18 : .1)
  const targetY = targetFocusY + pointerY * .07 + (thinking ? .045 : 0)
  lookAtTarget.position.x += (targetX - lookAtTarget.position.x) * .045
  lookAtTarget.position.y += (targetY - lookAtTarget.position.y) * .045
}

function animate(now = performance.now()) {
  frameId = requestAnimationFrame(animate)
  if (!renderer || !scene || !camera || !avatarVrm) return
  const frameInterval = lowPower.value ? 33 : 16
  if (now - lastFrameAt < frameInterval) return
  const delta = Math.min(.05, Math.max(.001, (now - lastFrameAt) / 1000))
  lastFrameAt = now
  const elapsed = (now - startedAt) / 1000

  animationMixer?.update(delta)
  applyStateMotion(elapsed)
  updateLookAt(elapsed)
  updateExpressions(now, elapsed)
  avatarVrm.update(delta)
  renderer.render(scene, camera)
}

function handlePointer(event) {
  if (!stageRef.value) return
  const rect = stageRef.value.getBoundingClientRect()
  pointerX = Math.max(-1, Math.min(1, (event.clientX - rect.left) / Math.max(1, rect.width) * 2 - 1))
  pointerY = Math.max(-1, Math.min(1, -((event.clientY - rect.top) / Math.max(1, rect.height) * 2 - 1)))
}

function handlePointerLeave() { pointerX = 0; pointerY = 0 }

function handleContextLost(event) {
  event.preventDefault()
  stageReady.value = false
  modelLoading.value = false
  loadFailed.value = true
  if (frameId) cancelAnimationFrame(frameId)
  emit('fallback', 'WebGL context lost')
}

async function initScene() {
  if (!stageRef.value || !canvasRef.value) return
  try {
    const [threeModule, loaderModule, vrmModule, animationModule] = await Promise.all([
      import('three'),
      import('three/addons/loaders/GLTFLoader.js'),
      import('@pixiv/three-vrm'),
      import('@pixiv/three-vrm-animation')
    ])
    THREE = threeModule
    VRMUtils = vrmModule.VRMUtils
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const constrainedDevice = (navigator.deviceMemory && navigator.deviceMemory <= 4) || (navigator.hardwareConcurrency && navigator.hardwareConcurrency <= 4)
    lowPower.value = props.performanceMode === 'reduced' || (props.performanceMode === 'auto' && (reduceMotion || constrainedDevice))

    scene = new THREE.Scene()
    scene.background = new THREE.Color(0x101426)
    scene.fog = new THREE.Fog(0x101426, 4.5, 10.5)
    camera = new THREE.PerspectiveCamera(29, 1, .1, 40)
    camera.position.set(0, 1.36, 2.5)
    renderer = new THREE.WebGLRenderer({ canvas: canvasRef.value, antialias: !lowPower.value, alpha: false, powerPreference: lowPower.value ? 'low-power' : 'high-performance' })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, lowPower.value ? 1.15 : 1.7))
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.shadowMap.enabled = !lowPower.value
    renderer.shadowMap.type = THREE.PCFSoftShadowMap
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.08

    createRoom()
    setupLighting()
    resize()
    resizeObserver = new ResizeObserver(resize)
    resizeObserver.observe(stageRef.value)
    stageRef.value.addEventListener('pointermove', handlePointer, { passive: true })
    stageRef.value.addEventListener('pointerleave', handlePointerLeave, { passive: true })
    canvasRef.value.addEventListener('webglcontextlost', handleContextLost)

    await loadAvatar(loaderModule.GLTFLoader, vrmModule.VRMLoaderPlugin)
    const animated = await loadIdleAnimation(loaderModule.GLTFLoader, animationModule.VRMAnimationLoaderPlugin, animationModule.createVRMAnimationClip, animationModule.VRMLookAtQuaternionProxy)
    modelProgress.value = 100
    modelLoading.value = false
    stageReady.value = true
    startedAt = performance.now()
    lastFrameAt = startedAt
    scheduleBlink(startedAt)
    emit('ready', { quality: lowPower.value ? 'reduced' : 'quality', model: 'vrm', animation: animated ? 'vrma' : 'procedural', name: avatarVrm.meta?.name || 'Anime interviewer' })
    animate(startedAt)
  } catch (error) {
    console.error('Unable to initialize VRM interviewer', error)
    stageReady.value = false
    modelLoading.value = false
    loadFailed.value = true
    emit('fallback', error instanceof Error ? error.message : 'VRM unavailable')
    cleanup(false)
  }
}

function cleanup(resetState = true) {
  if (frameId) cancelAnimationFrame(frameId)
  resizeObserver?.disconnect()
  stageRef.value?.removeEventListener('pointermove', handlePointer)
  stageRef.value?.removeEventListener('pointerleave', handlePointerLeave)
  canvasRef.value?.removeEventListener('webglcontextlost', handleContextLost)
  animationMixer?.stopAllAction()
  if (avatarVrm && VRMUtils) VRMUtils.deepDispose(avatarVrm.scene)
  if (scene) {
    scene.traverse(object => {
      if (object === avatarVrm?.scene || object.parent === avatarVrm?.scene) return
      object.geometry?.dispose?.()
      if (Array.isArray(object.material)) object.material.forEach(value => value.dispose?.())
      else object.material?.dispose?.()
    })
  }
  disposables.forEach(value => value?.dispose?.())
  renderer?.dispose()
  renderer = null
  scene = null
  camera = null
  avatarVrm = null
  animationMixer = null
  lookAtTarget = null
  Object.keys(bones).forEach(key => delete bones[key])
  disposables.length = 0
  if (resetState) stageReady.value = false
}

onMounted(initScene)
onBeforeUnmount(cleanup)
</script>

<style scoped>
.virtual-interviewer { position: absolute; inset: 0; overflow: hidden; background: #101426; }
.virtual-interviewer canvas { width: 100%; height: 100%; display: block; opacity: 0; transform: scale(1.015); transition: opacity .65s ease, transform 1s cubic-bezier(.2,.8,.2,1); }
.virtual-interviewer.is-ready canvas { opacity: 1; transform: scale(1); }
.scene-atmosphere { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.scene-atmosphere i { position: absolute; border-radius: 50%; filter: blur(1px); opacity: .26; animation: drift 9s ease-in-out infinite; }
.scene-atmosphere i:first-child { width: 180px; height: 180px; top: 9%; right: 12%; background: radial-gradient(circle, rgba(122,146,255,.3), transparent 70%); }
.scene-atmosphere i:nth-child(2) { width: 110px; height: 110px; top: 42%; left: 8%; background: radial-gradient(circle, rgba(255,174,132,.22), transparent 70%); animation-delay: -3s; }
.scene-atmosphere i:last-child { width: 230px; height: 230px; bottom: -12%; left: 34%; background: radial-gradient(circle, rgba(109,93,201,.18), transparent 68%); animation-delay: -6s; }
.scene-vignette { position: absolute; inset: 0; pointer-events: none; background: radial-gradient(circle at 50% 39%, transparent 36%, rgba(7,9,22,.12) 68%, rgba(5,7,18,.52) 100%), linear-gradient(180deg, rgba(5,8,20,.18), transparent 42%, rgba(5,7,17,.42)); }
.model-loader { position: absolute; inset: 0; z-index: 2; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px; background: radial-gradient(circle at 50% 34%, rgba(102,111,196,.28), transparent 31%), linear-gradient(145deg, #171b35, #0d1022); color: #f4f5ff; text-align: center; transition: opacity .35s; }
.loader-portrait { position: relative; width: 106px; height: 106px; margin-bottom: 17px; }
.loader-portrait::before { content: ''; position: absolute; inset: -9px; border: 1px solid rgba(179,188,255,.24); border-radius: 31px; transform: rotate(7deg); animation: orbit 5s linear infinite; }
.loader-portrait img { width: 100%; height: 100%; display: block; object-fit: cover; border: 1px solid rgba(211,216,255,.35); border-radius: 27px; box-shadow: 0 20px 50px rgba(3,5,16,.42); }
.loader-portrait > span { position: absolute; right: -7px; bottom: -6px; display: flex; align-items: center; gap: 3px; height: 24px; padding: 0 7px; border: 1px solid rgba(211,216,255,.24); border-radius: 999px; background: rgba(14,17,38,.88); }
.loader-portrait > span i { width: 4px; height: 4px; border-radius: 50%; background: #aab7ff; animation: loaderDot .9s infinite alternate; }.loader-portrait > span i:nth-child(2) { animation-delay: .16s; }.loader-portrait > span i:nth-child(3) { animation-delay: .32s; }
.model-loader strong { font-size: 16px; letter-spacing: .02em; }.model-loader small { margin-top: 6px; color: rgba(226,230,255,.62); font-size: 10px; }
.loader-track { width: min(210px, 58vw); height: 3px; margin-top: 20px; overflow: hidden; border-radius: 999px; background: rgba(221,225,255,.13); }.loader-track i { height: 100%; display: block; border-radius: inherit; background: linear-gradient(90deg, #7c8cf1, #c0a5ff); box-shadow: 0 0 16px rgba(145,158,255,.55); transition: width .24s ease; }
.model-loader em { margin-top: 7px; color: rgba(226,230,255,.46); font: 700 8px ui-monospace, monospace; font-style: normal; }
.avatar-fallback { position: absolute; inset: 0; z-index: 2; display: flex; flex-direction: column; align-items: center; justify-content: center; overflow: hidden; padding: 30px; background: radial-gradient(circle at 50% 37%, rgba(112,121,211,.34), transparent 27%), linear-gradient(150deg, #171b36, #0b0e20); color: #f5f6ff; text-align: center; }
.fallback-halo { position: absolute; width: 310px; height: 310px; border: 1px solid rgba(189,197,255,.12); border-radius: 50%; box-shadow: 0 0 0 38px rgba(172,182,255,.025), 0 0 0 84px rgba(172,182,255,.018); animation: haloPulse 4s ease-in-out infinite; }
.avatar-fallback img { position: relative; width: 134px; height: 134px; object-fit: cover; border: 1px solid rgba(216,220,255,.34); border-radius: 36px; box-shadow: 0 24px 62px rgba(3,5,16,.45); }.avatar-fallback strong { position: relative; margin-top: 19px; font-size: 16px; }.avatar-fallback small { position: relative; max-width: 310px; margin-top: 7px; color: rgba(226,230,255,.58); font-size: 10px; line-height: 1.55; }
.avatar-signature { position: absolute; top: 86px; left: 19px; display: flex; align-items: center; gap: 5px; padding: 5px 7px; border: 1px solid rgba(202,208,255,.12); border-radius: 999px; background: rgba(9,12,29,.36); color: rgba(226,230,255,.48); font: 700 7px ui-monospace, monospace; letter-spacing: .08em; backdrop-filter: blur(10px); }.avatar-signature i { width: 5px; height: 5px; border-radius: 50%; background: #9cadff; box-shadow: 0 0 0 3px rgba(156,173,255,.1); }
.state-speaking .avatar-signature i { animation: signaturePulse .8s infinite; }
@keyframes drift { 50% { transform: translate3d(10px,-12px,0) scale(1.08); opacity: .4; } }
@keyframes orbit { to { transform: rotate(367deg); } }
@keyframes loaderDot { to { transform: translateY(-3px); opacity: .4; } }
@keyframes haloPulse { 50% { transform: scale(1.045); opacity: .68; } }
@keyframes signaturePulse { 50% { opacity: .35; transform: scale(.72); } }
@media (max-width: 600px) { .avatar-signature { top: 65px; left: 14px; }.loader-portrait { width: 92px; height: 92px; } }
@media (prefers-reduced-motion: reduce) { .scene-atmosphere i, .loader-portrait::before, .fallback-halo { animation: none; } }
</style>
