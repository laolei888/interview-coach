<template>
  <div ref="stageRef" class="orb-scene" aria-hidden="true">
    <canvas ref="canvasRef"></canvas>
    <div v-if="!webglReady" class="orb-fallback">
      <span></span><i></i><b></b>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const stageRef = ref(null)
const canvasRef = ref(null)
const webglReady = ref(true)
let THREE
let renderer
let scene
let camera
let frameId = 0
let resizeObserver
const objects = []

function createRing(radius, color, rotation) {
  const geometry = new THREE.TorusGeometry(radius, 0.012, 12, 140)
  const material = new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.42 })
  const mesh = new THREE.Mesh(geometry, material)
  mesh.rotation.set(rotation[0], rotation[1], rotation[2])
  scene.add(mesh)
  objects.push(mesh)
  return mesh
}

async function initScene() {
  if (!stageRef.value || !canvasRef.value) return
  try {
    THREE = await import('three')
    scene = new THREE.Scene()
    camera = new THREE.PerspectiveCamera(42, 1, 0.1, 100)
    camera.position.set(0, 0.18, 4.6)

    renderer = new THREE.WebGLRenderer({ canvas: canvasRef.value, antialias: true, alpha: true })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.8))
    renderer.setClearColor(0x000000, 0)

    const sphereGeometry = new THREE.IcosahedronGeometry(1.05, 5)
    const sphereMaterial = new THREE.MeshStandardMaterial({
      color: 0x5fc1b6,
      roughness: 0.42,
      metalness: 0.24,
      transparent: true,
      opacity: 0.94
    })
    const sphere = new THREE.Mesh(sphereGeometry, sphereMaterial)
    scene.add(sphere)
    objects.push(sphere)

    const wire = new THREE.Mesh(
      new THREE.IcosahedronGeometry(1.075, 2),
      new THREE.MeshBasicMaterial({ color: 0xffffff, wireframe: true, transparent: true, opacity: 0.12 })
    )
    scene.add(wire)
    objects.push(wire)

    createRing(1.42, 0xffffff, [Math.PI / 2.7, 0.2, 0.45])
    createRing(1.7, 0xf08b73, [Math.PI / 2.2, 0.8, -0.25])
    createRing(1.96, 0x86efe2, [Math.PI / 1.9, -0.35, 0.18])

    const light = new THREE.DirectionalLight(0xffffff, 2.4)
    light.position.set(2, 3, 4)
    scene.add(light)
    scene.add(new THREE.AmbientLight(0x9ee8df, 1.2))

    resize()
    resizeObserver = new ResizeObserver(resize)
    resizeObserver.observe(stageRef.value)
    animate()
  } catch (error) {
    webglReady.value = false
    cleanup()
  }
}

function resize() {
  if (!renderer || !stageRef.value || !camera) return
  const { width, height } = stageRef.value.getBoundingClientRect()
  renderer.setSize(Math.max(1, width), Math.max(1, height), false)
  camera.aspect = Math.max(1, width) / Math.max(1, height)
  camera.updateProjectionMatrix()
}

function animate() {
  frameId = requestAnimationFrame(animate)
  objects.forEach((object, index) => {
    object.rotation.x += 0.002 + index * 0.0005
    object.rotation.y += 0.003 + index * 0.0007
  })
  renderer?.render(scene, camera)
}

function cleanup() {
  if (frameId) cancelAnimationFrame(frameId)
  resizeObserver?.disconnect()
  if (scene) {
    scene.traverse(object => {
      object.geometry?.dispose?.()
      if (Array.isArray(object.material)) object.material.forEach(item => item.dispose?.())
      else object.material?.dispose?.()
    })
  }
  renderer?.dispose()
  renderer = null
  scene = null
  camera = null
  objects.length = 0
}

onMounted(initScene)
onBeforeUnmount(cleanup)
</script>

<style scoped>
.orb-scene { position: absolute; inset: 0; overflow: hidden; pointer-events: none; }
.orb-scene canvas { width: 100%; height: 100%; display: block; }
.orb-fallback { position: absolute; inset: 0; display: grid; place-items: center; }
.orb-fallback span,.orb-fallback i,.orb-fallback b { position: absolute; display: block; border: 1px solid rgba(255,255,255,.2); border-radius: 50%; }
.orb-fallback span { width: 210px; height: 210px; background: rgba(95,193,182,.16); }
.orb-fallback i { width: 286px; height: 96px; transform: rotate(-18deg); }
.orb-fallback b { width: 340px; height: 128px; transform: rotate(24deg); border-color: rgba(240,139,115,.28); }
</style>
