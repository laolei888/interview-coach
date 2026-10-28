<template>
  <div
    ref="stageRef"
    :class="['virtual-interviewer', `state-${state}`, { 'is-ready': webglReady, 'is-low-power': lowPower }]"
    :aria-label="`三维虚拟面试官，当前状态：${stateLabel}`"
    role="img"
  >
    <canvas ref="canvasRef"></canvas>
    <div class="scene-vignette" aria-hidden="true"></div>
    <div v-if="!webglReady" class="avatar-fallback" aria-hidden="true">
      <div class="fallback-room"><i></i><i></i><i></i></div>
      <div class="fallback-person">
        <span class="fallback-head"><i></i><i></i><b></b></span>
        <span class="fallback-body"></span>
        <span class="fallback-arm left"></span>
        <span class="fallback-arm right"></span>
      </div>
    </div>
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
  }
})

const emit = defineEmits(['ready', 'fallback'])
const stageRef = ref(null)
const canvasRef = ref(null)
const webglReady = ref(false)
const stateLabel = computed(() => ({
  preparing: '正在准备',
  thinking: '正在思考',
  speaking: '正在提问',
  listening: '正在倾听',
  idle: '等待回答',
  ended: '面试结束',
  error: '连接异常'
}[props.state] || '等待回答'))

let THREE
let renderer
let scene
let camera
let resizeObserver
let frameId = 0
let startedAt = 0
let lastFrameAt = 0
let nextBlinkAt = 0
let blinkStartedAt = 0
let pointerX = 0
let pointerY = 0
let lowPower = false
const disposables = []
const rig = {}

const material = (color, options = {}) => {
  const value = new THREE.MeshStandardMaterial({ color, roughness: .68, metalness: .02, ...options })
  disposables.push(value)
  return value
}

const addMesh = (geometry, meshMaterial, parent = scene) => {
  disposables.push(geometry)
  const mesh = new THREE.Mesh(geometry, meshMaterial)
  mesh.castShadow = !lowPower
  mesh.receiveShadow = !lowPower
  parent.add(mesh)
  return mesh
}

const makeBox = (size, color, position, parent = scene, options = {}) => {
  const mesh = addMesh(new THREE.BoxGeometry(...size), material(color, options), parent)
  mesh.position.set(...position)
  return mesh
}

const makeSphere = (radius, color, position, parent = scene, scale = [1, 1, 1], options = {}) => {
  const segments = lowPower ? 20 : 32
  const mesh = addMesh(new THREE.SphereGeometry(radius, segments, Math.max(14, Math.round(segments * .7))), material(color, options), parent)
  mesh.position.set(...position)
  mesh.scale.set(...scale)
  return mesh
}

function createRoom() {
  const wall = makeBox([8.4, 4.8, .16], 0x17352f, [0, 2.05, -1.85])
  wall.receiveShadow = true
  const floor = makeBox([8.6, .12, 7.2], 0x0d1d1a, [0, -.02, .4])
  floor.receiveShadow = true

  makeBox([2.55, 1.72, .05], 0xb9d7d0, [2.26, 2.62, -1.74], scene, { emissive: 0x365d55, emissiveIntensity: .42 })
  makeBox([.055, 1.72, .08], 0x315d54, [2.26, 2.62, -1.67])
  makeBox([2.55, .055, .08], 0x315d54, [2.26, 2.62, -1.67])
  makeBox([.055, 1.72, .08], 0x315d54, [1.42, 2.62, -1.67])
  makeBox([.055, 1.72, .08], 0x315d54, [3.1, 2.62, -1.67])
  makeBox([1.45, .9, .08], 0x204740, [-2.35, 2.65, -1.69])
  makeBox([1.16, .045, .09], 0x79c8ba, [-2.35, 2.78, -1.61], scene, { emissive: 0x2c7d70, emissiveIntensity: .25 })

  const plantPot = addMesh(new THREE.CylinderGeometry(.19, .25, .38, 20), material(0x8f5f45))
  plantPot.position.set(-2.9, .34, -1.1)
  for (let index = 0; index < 6; index += 1) {
    const leaf = makeSphere(.2, index % 2 ? 0x3f8b69 : 0x58a879, [-2.9 + Math.sin(index) * .2, .72 + (index % 3) * .17, -1.1 + Math.cos(index) * .08], scene, [.52, 1.4, .34])
    leaf.rotation.z = (index - 2.5) * .18
  }

  const desk = makeBox([4.6, .18, 1.55], 0x513b30, [0, .63, .72])
  desk.receiveShadow = true
  makeBox([.18, 1.25, 1.15], 0x352c27, [-1.86, .05, .72])
  makeBox([.18, 1.25, 1.15], 0x352c27, [1.86, .05, .72])
  makeBox([.72, .025, .5], 0xf1eee5, [.82, .74, .49], scene, { roughness: .9 })
  makeBox([.38, .02, .015], 0x6ca497, [.75, .765, .35], scene, { emissive: 0x244c44, emissiveIntensity: .16 })

  const chair = makeBox([1.22, 1.85, .28], 0x152925, [0, 1.03, -1.18])
  chair.rotation.x = -.04
}

function createArm(side, jacketMaterial, skinMaterial) {
  const sign = side === 'left' ? -1 : 1
  const shoulder = new THREE.Group()
  shoulder.position.set(sign * .57, 1.56, -.02)
  rig.body.add(shoulder)

  const upper = addMesh(new THREE.CylinderGeometry(.13, .17, .68, lowPower ? 14 : 22), jacketMaterial, shoulder)
  upper.position.y = -.29
  upper.rotation.z = sign * -.08
  const forearm = addMesh(new THREE.CylinderGeometry(.09, .12, .58, lowPower ? 14 : 22), skinMaterial, shoulder)
  forearm.position.set(sign * .08, -.83, .18)
  forearm.rotation.z = sign * -.22
  forearm.rotation.x = -.35
  const hand = addMesh(new THREE.SphereGeometry(.115, lowPower ? 14 : 22, 16), skinMaterial, shoulder)
  hand.position.set(sign * .14, -1.11, .29)
  hand.scale.set(.76, 1.12, .62)

  shoulder.rotation.z = sign * .22
  shoulder.rotation.x = -.18
  return shoulder
}

function createAvatar() {
  const skin = material(0xd8a17f, { roughness: .8 })
  const skinSoft = material(0xe6b28f, { roughness: .82 })
  const jacket = material(0x1c3c37, { roughness: .7 })
  const shirt = material(0xe5ece8, { roughness: .88 })
  const hair = material(0x171b1b, { roughness: .9 })
  const eyeWhite = material(0xf8fbfa, { roughness: .4 })
  const iris = material(0x203a34, { roughness: .32 })
  const mouthMaterial = material(0x7f3f3a, { roughness: .55, emissive: 0x30100e, emissiveIntensity: .2 })

  rig.avatar = new THREE.Group()
  rig.avatar.position.set(0, .18, -.18)
  scene.add(rig.avatar)

  rig.body = new THREE.Group()
  rig.avatar.add(rig.body)
  const torso = addMesh(new THREE.CylinderGeometry(.43, .64, 1.18, lowPower ? 22 : 36), jacket, rig.body)
  torso.position.y = 1.16
  torso.scale.z = .64
  const shirtFront = makeBox([.34, .76, .035], 0xe5ece8, [0, 1.32, .43], rig.body)
  shirtFront.rotation.x = -.08
  const lapelLeft = makeBox([.21, .58, .045], 0x274e47, [-.2, 1.38, .46], rig.body)
  lapelLeft.rotation.z = -.28
  const lapelRight = makeBox([.21, .58, .045], 0x274e47, [.2, 1.38, .46], rig.body)
  lapelRight.rotation.z = .28
  makeBox([.055, .43, .04], 0x315f57, [0, 1.3, .49], rig.body)

  rig.neck = addMesh(new THREE.CylinderGeometry(.15, .18, .24, 24), skin, rig.body)
  rig.neck.position.y = 1.82

  rig.headGroup = new THREE.Group()
  rig.headGroup.position.set(0, 2.13, .03)
  rig.body.add(rig.headGroup)
  const head = makeSphere(.42, 0xd8a17f, [0, 0, .04], rig.headGroup, [.88, 1.12, .83])
  head.castShadow = true
  const hairBack = makeSphere(.425, 0x171b1b, [0, .105, -.035], rig.headGroup, [.92, .95, .86])
  hairBack.position.z = -.055
  const hairFront = makeBox([.63, .18, .19], 0x171b1b, [0, .31, .25], rig.headGroup)
  hairFront.rotation.x = -.13
  makeSphere(.095, 0xd8a17f, [-.39, -.01, .04], rig.headGroup, [.48, 1, .6])
  makeSphere(.095, 0xd8a17f, [.39, -.01, .04], rig.headGroup, [.48, 1, .6])

  rig.leftEye = makeSphere(.07, 0xf8fbfa, [-.145, .07, .37], rig.headGroup, [1.05, .7, .45])
  rig.rightEye = makeSphere(.07, 0xf8fbfa, [.145, .07, .37], rig.headGroup, [1.05, .7, .45])
  rig.leftPupil = makeSphere(.032, 0x203a34, [-.145, .07, .414], rig.headGroup, [.72, 1, .45])
  rig.rightPupil = makeSphere(.032, 0x203a34, [.145, .07, .414], rig.headGroup, [.72, 1, .45])
  const leftBrow = makeBox([.19, .027, .03], 0x2a2522, [-.15, .18, .405], rig.headGroup)
  leftBrow.rotation.z = .05
  const rightBrow = makeBox([.19, .027, .03], 0x2a2522, [.15, .18, .405], rig.headGroup)
  rightBrow.rotation.z = -.05
  const nose = addMesh(new THREE.ConeGeometry(.042, .16, 16), skinSoft, rig.headGroup)
  nose.position.set(0, -.005, .43)
  nose.rotation.x = Math.PI / 2
  rig.mouth = makeBox([.19, .025, .025], 0x7f3f3a, [0, -.17, .405], rig.headGroup)
  rig.mouth.material = mouthMaterial

  rig.leftArm = createArm('left', jacket, skin)
  rig.rightArm = createArm('right', jacket, skin)
  rig.baseBodyY = rig.body.position.y

  const badge = makeBox([.18, .12, .02], 0xd9efe9, [.41, 1.48, .49], rig.body, { emissive: 0x2d6f63, emissiveIntensity: .16 })
  badge.rotation.z = .02
}

function setupLighting() {
  scene.add(new THREE.HemisphereLight(0xd6f5ef, 0x13201d, lowPower ? 1.55 : 1.85))
  const key = new THREE.DirectionalLight(0xffefe4, lowPower ? 2.1 : 2.8)
  key.position.set(-2.4, 4.6, 3.4)
  key.castShadow = !lowPower
  if (!lowPower) {
    key.shadow.mapSize.set(1024, 1024)
    key.shadow.camera.left = -3
    key.shadow.camera.right = 3
    key.shadow.camera.top = 4
    key.shadow.camera.bottom = -1
  }
  scene.add(key)
  const rim = new THREE.PointLight(0x72d9c8, 18, 7)
  rim.position.set(2.6, 2.8, 1.2)
  scene.add(rim)
  const warm = new THREE.PointLight(0xf5a283, 9, 5)
  warm.position.set(-2.8, 1.8, .8)
  scene.add(warm)
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
  nextBlinkAt = now + 2300 + Math.random() * 2600
  blinkStartedAt = 0
}

function animate(now = performance.now()) {
  frameId = requestAnimationFrame(animate)
  if (!renderer || !scene || !camera || !rig.avatar) return
  if (lowPower && now - lastFrameAt < 32) return
  lastFrameAt = now
  const t = (now - startedAt) / 1000
  const speaking = props.state === 'speaking'
  const listening = props.state === 'listening'
  const thinking = props.state === 'thinking' || props.state === 'preparing'
  const ended = props.state === 'ended'

  if (now > nextBlinkAt && !blinkStartedAt) blinkStartedAt = now
  let eyeScale = .7
  if (blinkStartedAt) {
    const blinkTime = now - blinkStartedAt
    eyeScale = blinkTime < 90 ? Math.max(.05, .7 * (1 - blinkTime / 90)) : Math.min(.7, .7 * ((blinkTime - 90) / 110))
    if (blinkTime > 200) scheduleBlink(now)
  }
  rig.leftEye.scale.y = eyeScale
  rig.rightEye.scale.y = eyeScale
  rig.leftPupil.scale.y = Math.max(.12, eyeScale / .7)
  rig.rightPupil.scale.y = Math.max(.12, eyeScale / .7)

  const gazeX = pointerX * .018 + (thinking ? Math.sin(t * .7) * .012 : 0)
  const gazeY = pointerY * .012 + (thinking ? .01 : 0)
  rig.leftPupil.position.x = -.145 + gazeX
  rig.rightPupil.position.x = .145 + gazeX
  rig.leftPupil.position.y = .07 + gazeY
  rig.rightPupil.position.y = .07 + gazeY

  const mouthOpen = speaking ? .8 + Math.abs(Math.sin(t * 10.7) + Math.sin(t * 6.1) * .45) * 4.2 : 1
  rig.mouth.scale.y += (mouthOpen - rig.mouth.scale.y) * .38
  rig.mouth.scale.x += ((speaking ? 1.08 : 1) - rig.mouth.scale.x) * .2

  const headTargetX = listening ? Math.sin(t * .85) * .025 + Math.max(0, Math.sin(t * .42)) * .035 : speaking ? Math.sin(t * 1.1) * .018 : thinking ? -.035 : ended ? .02 : 0
  const headTargetY = thinking ? -.12 + Math.sin(t * .5) * .04 : speaking ? Math.sin(t * .72) * .035 : pointerX * .035
  rig.headGroup.rotation.x += (headTargetX - rig.headGroup.rotation.x) * .06
  rig.headGroup.rotation.y += (headTargetY - rig.headGroup.rotation.y) * .06
  rig.headGroup.rotation.z += ((speaking ? Math.sin(t * .52) * .012 : 0) - rig.headGroup.rotation.z) * .05

  const breathe = Math.sin(t * 1.45) * .009
  rig.body.position.y = rig.baseBodyY + breathe
  const leftZ = speaking ? -.22 + Math.sin(t * 1.8) * .13 : listening ? -.12 : .22
  const rightZ = speaking ? .22 + Math.sin(t * 1.55 + 1.4) * .13 : listening ? .12 : -.22
  const armX = speaking ? -.48 + Math.sin(t * 1.35) * .08 : listening ? -.28 : thinking ? -.36 : -.18
  rig.leftArm.rotation.z += (leftZ - rig.leftArm.rotation.z) * .055
  rig.rightArm.rotation.z += (rightZ - rig.rightArm.rotation.z) * .055
  rig.leftArm.rotation.x += (armX - rig.leftArm.rotation.x) * .055
  rig.rightArm.rotation.x += ((armX + (speaking ? .1 : 0)) - rig.rightArm.rotation.x) * .055
  rig.avatar.rotation.y = Math.sin(t * .28) * .006

  camera.position.x = Math.sin(t * .15) * .018
  camera.position.y = 1.72 + Math.sin(t * .2) * .009
  camera.lookAt(0, 1.35, -.12)
  renderer.render(scene, camera)
}

function handlePointer(event) {
  if (!stageRef.value) return
  const rect = stageRef.value.getBoundingClientRect()
  pointerX = Math.max(-1, Math.min(1, (event.clientX - rect.left) / Math.max(1, rect.width) * 2 - 1))
  pointerY = Math.max(-1, Math.min(1, -((event.clientY - rect.top) / Math.max(1, rect.height) * 2 - 1)))
}

function handleContextLost(event) {
  event.preventDefault()
  webglReady.value = false
  if (frameId) cancelAnimationFrame(frameId)
  emit('fallback', 'WebGL context lost')
}

async function initScene() {
  if (!stageRef.value || !canvasRef.value) return
  try {
    THREE = await import('three')
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches
    const constrainedDevice = (navigator.deviceMemory && navigator.deviceMemory <= 4) || (navigator.hardwareConcurrency && navigator.hardwareConcurrency <= 4)
    lowPower = props.performanceMode === 'reduced' || (props.performanceMode === 'auto' && (reduceMotion || constrainedDevice))
    scene = new THREE.Scene()
    scene.background = new THREE.Color(0x0e211d)
    scene.fog = new THREE.Fog(0x0e211d, 4.8, 9.5)
    camera = new THREE.PerspectiveCamera(34, 1, .1, 40)
    camera.position.set(0, 1.72, 4.5)

    renderer = new THREE.WebGLRenderer({ canvas: canvasRef.value, antialias: !lowPower, alpha: false, powerPreference: lowPower ? 'low-power' : 'high-performance' })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, lowPower ? 1.2 : 1.75))
    renderer.outputColorSpace = THREE.SRGBColorSpace
    renderer.shadowMap.enabled = !lowPower
    renderer.shadowMap.type = THREE.PCFSoftShadowMap
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.05

    createRoom()
    createAvatar()
    setupLighting()
    resize()
    resizeObserver = new ResizeObserver(resize)
    resizeObserver.observe(stageRef.value)
    stageRef.value.addEventListener('pointermove', handlePointer, { passive: true })
    canvasRef.value.addEventListener('webglcontextlost', handleContextLost)
    startedAt = performance.now()
    scheduleBlink(startedAt)
    webglReady.value = true
    emit('ready', { quality: lowPower ? 'reduced' : 'quality' })
    animate(startedAt)
  } catch (error) {
    webglReady.value = false
    emit('fallback', error instanceof Error ? error.message : 'WebGL unavailable')
    cleanup()
  }
}

function cleanup() {
  if (frameId) cancelAnimationFrame(frameId)
  resizeObserver?.disconnect()
  stageRef.value?.removeEventListener('pointermove', handlePointer)
  canvasRef.value?.removeEventListener('webglcontextlost', handleContextLost)
  if (scene) {
    scene.traverse(object => {
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
  Object.keys(rig).forEach(key => delete rig[key])
  disposables.length = 0
}

onMounted(initScene)
onBeforeUnmount(cleanup)
</script>

<style scoped>
.virtual-interviewer { position: absolute; inset: 0; overflow: hidden; background: #0e211d; }
.virtual-interviewer canvas { width: 100%; height: 100%; display: block; opacity: 0; transition: opacity .45s ease; }
.virtual-interviewer.is-ready canvas { opacity: 1; }
.scene-vignette { position: absolute; inset: 0; pointer-events: none; background: radial-gradient(circle at 50% 40%, transparent 42%, rgba(4, 13, 11, .15) 72%, rgba(4, 13, 11, .54) 100%), linear-gradient(180deg, rgba(3, 12, 10, .08), transparent 44%, rgba(3, 12, 10, .38)); }
.avatar-fallback { position: absolute; inset: 0; overflow: hidden; background: radial-gradient(circle at 50% 34%, #315d54, transparent 31%), linear-gradient(160deg, #183a33, #0b1916); }
.fallback-room i { position: absolute; background: rgba(190, 232, 222, .11); border: 1px solid rgba(190, 232, 222, .14); }
.fallback-room i:first-child { width: 30%; height: 34%; right: 9%; top: 9%; }
.fallback-room i:nth-child(2) { width: 23%; height: 17%; left: 8%; top: 15%; }
.fallback-room i:last-child { width: 120%; height: 20%; left: -10%; bottom: -2%; background: #3b3029; }
.fallback-person { position: absolute; left: 50%; bottom: 12%; width: 220px; height: 360px; transform: translateX(-50%); }
.fallback-head { position: absolute; z-index: 2; top: 0; left: 50%; width: 112px; height: 138px; border-radius: 48% 48% 45% 45%; background: #d8a17f; transform: translateX(-50%); box-shadow: inset 0 26px #1b211f; }
.fallback-head i { position: absolute; top: 70px; width: 13px; height: 8px; border-radius: 50%; background: #1f3731; animation: fallbackBlink 4s infinite; }
.fallback-head i:first-child { left: 26px; }.fallback-head i:nth-child(2) { right: 26px; }
.fallback-head b { position: absolute; left: 50%; bottom: 25px; width: 30px; height: 4px; border-radius: 50%; background: #7f3f3a; transform: translateX(-50%); }
.fallback-body { position: absolute; left: 50%; top: 126px; width: 190px; height: 230px; border-radius: 48% 48% 12px 12px; background: linear-gradient(90deg, #17342f, #2a544c 50%, #17342f); transform: translateX(-50%); }
.fallback-arm { position: absolute; top: 155px; width: 45px; height: 178px; border-radius: 40px; background: #1d4039; transform-origin: top; }
.fallback-arm.left { left: 5px; transform: rotate(12deg); }.fallback-arm.right { right: 5px; transform: rotate(-12deg); }
.state-speaking .fallback-head b { animation: fallbackTalk .22s infinite alternate; }
@keyframes fallbackTalk { to { height: 14px; border-radius: 50%; } }
@keyframes fallbackBlink { 0%,45%,48%,100% { transform: scaleY(1); } 46%,47% { transform: scaleY(.1); } }
@media (max-width: 700px) { .fallback-person { bottom: 7%; transform: translateX(-50%) scale(.86); transform-origin: bottom; } }
</style>
