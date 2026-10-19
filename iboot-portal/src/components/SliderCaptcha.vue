<template>
  <el-dialog
    v-model="visible"
    :title="t('login.captcha.title')"
    width="340px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="slider-captcha-dialog"
  >
    <div class="captcha-container">
      <div class="captcha-tip" :class="{ success: isSuccess, error: isError }">
        {{ tipText }}
      </div>
      <div class="captcha-track" ref="trackRef">
        <div class="captcha-fill" :style="{ width: fillWidth + 'px' }"></div>
        <div
          class="captcha-target"
          :style="{ left: targetPosition + 'px' }"
        ></div>
        <div
          class="captcha-slider"
          :style="{ left: sliderPosition + 'px' }"
          :class="{ success: isSuccess, error: isError, dragging: isDragging }"
          @mousedown="handleMouseDown"
          @touchstart="handleTouchStart"
        >
          <el-icon v-if="isSuccess"><Check /></el-icon>
          <el-icon v-else-if="isError"><Close /></el-icon>
          <el-icon v-else><Right /></el-icon>
        </div>
      </div>
      <div class="captcha-actions">
        <el-button size="small" @click="handleReset" :disabled="isSuccess">
          <el-icon><Refresh /></el-icon>
          {{ t("login.captcha.refresh") }}
        </el-button>
        <el-button size="small" @click="handleCancel">
          {{ t("login.captcha.cancel") }}
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useI18n } from "vue-i18n";
import { Check, Close, Right, Refresh } from "@element-plus/icons-vue";

const { t } = useI18n();

const props = defineProps<{
  modelValue: boolean;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: boolean): void;
  (e: "success"): void;
  (e: "cancel"): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit("update:modelValue", val),
});

const trackRef = ref<HTMLElement | null>(null);
const trackWidth = 280;
const sliderWidth = 40;
const targetWidth = 8;
const tolerance = 5;

const sliderPosition = ref(0);
const targetPosition = ref(0);
const fillWidth = ref(0);
const isDragging = ref(false);
const isSuccess = ref(false);
const isError = ref(false);
const startX = ref(0);

const tipText = computed(() => {
  if (isSuccess.value) return t("login.captcha.tip_success");
  if (isError.value) return t("login.captcha.tip_error");
  return t("login.captcha.tip_default");
});

const generateTargetPosition = () => {
  const minPos = 100;
  const maxPos = trackWidth - sliderWidth - targetWidth - 20;
  targetPosition.value =
    Math.floor(Math.random() * (maxPos - minPos + 1)) + minPos;
};

const handleMouseDown = (e: MouseEvent) => {
  if (isSuccess.value) return;
  isDragging.value = true;
  isError.value = false;
  startX.value = e.clientX - sliderPosition.value;
  document.addEventListener("mousemove", handleMouseMove);
  document.addEventListener("mouseup", handleMouseUp);
};

const handleTouchStart = (e: TouchEvent) => {
  if (isSuccess.value) return;
  isDragging.value = true;
  isError.value = false;
  startX.value = e.touches[0].clientX - sliderPosition.value;
  document.addEventListener("touchmove", handleTouchMove);
  document.addEventListener("touchend", handleTouchEnd);
};

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return;
  moveSlider(e.clientX);
};

const handleTouchMove = (e: TouchEvent) => {
  if (!isDragging.value) return;
  moveSlider(e.touches[0].clientX);
};

const moveSlider = (clientX: number) => {
  let newPos = clientX - startX.value;
  const maxPos = trackWidth - sliderWidth;
  newPos = Math.max(0, Math.min(newPos, maxPos));
  sliderPosition.value = newPos;
  fillWidth.value = newPos + sliderWidth / 2;
};

const handleMouseUp = () => {
  finishDrag();
  document.removeEventListener("mousemove", handleMouseMove);
  document.removeEventListener("mouseup", handleMouseUp);
};

const handleTouchEnd = () => {
  finishDrag();
  document.removeEventListener("touchmove", handleTouchMove);
  document.removeEventListener("touchend", handleTouchEnd);
};

const finishDrag = () => {
  if (!isDragging.value) return;
  isDragging.value = false;

  const sliderCenter = sliderPosition.value + sliderWidth / 2;
  const targetCenter = targetPosition.value + targetWidth / 2;

  if (Math.abs(sliderCenter - targetCenter) <= tolerance) {
    isSuccess.value = true;
    setTimeout(() => {
      emit("success");
      visible.value = false;
    }, 500);
  } else {
    isError.value = true;
    setTimeout(() => {
      resetSlider();
    }, 1000);
  }
};

const resetSlider = () => {
  sliderPosition.value = 0;
  fillWidth.value = 0;
  isError.value = false;
  isSuccess.value = false;
  generateTargetPosition();
};

const handleReset = () => {
  resetSlider();
};

const handleCancel = () => {
  emit("cancel");
  visible.value = false;
  resetSlider();
};

onMounted(() => {
  generateTargetPosition();
});

onUnmounted(() => {
  document.removeEventListener("mousemove", handleMouseMove);
  document.removeEventListener("mouseup", handleMouseUp);
  document.removeEventListener("touchmove", handleTouchMove);
  document.removeEventListener("touchend", handleTouchEnd);
});
</script>

<style scoped>
.captcha-container {
  padding: 10px;
}

.captcha-tip {
  text-align: center;
  margin-bottom: 15px;
  font-size: 14px;
  color: var(--ib-color-steel);
  transition: color 0.3s;
}

.captcha-tip.success {
  color: var(--ib-color-success-text);
}

.captcha-tip.error {
  color: var(--ib-color-danger-text);
}

.captcha-track {
  position: relative;
  width: 280px;
  height: 40px;
  background: var(--ib-color-surface);
  border-radius: 20px;
  border: 1px solid var(--ib-color-hairline);
  margin: 0 auto;
  overflow: hidden;
}

.captcha-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: var(--ib-color-brand-blue);
  border-radius: 20px 0 0 20px;
  transition: none;
}

.captcha-target {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 8px;
  height: 24px;
  background: var(--ib-color-danger-text);
  border-radius: 4px;
  box-shadow: var(--ib-shadow-1);
}

.captcha-slider {
  position: absolute;
  top: 0;
  width: 40px;
  height: 40px;
  background: var(--ib-color-canvas);
  border-radius: 50%;
  border: 2px solid var(--ib-color-brand-blue);
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ib-color-brand-blue);
  font-size: 18px;
  box-shadow: var(--ib-shadow-2);
  transition:
    border-color 0.3s,
    color 0.3s,
    transform 0.1s;
  user-select: none;
  z-index: 10;
}

.captcha-slider:hover {
  transform: scale(1.05);
}

.captcha-slider.dragging {
  cursor: grabbing;
  transform: scale(1.1);
}

.captcha-slider.success {
  border-color: var(--ib-color-success-text);
  color: var(--ib-color-success-text);
  background: var(--ib-color-success-bg);
}

.captcha-slider.error {
  border-color: var(--ib-color-danger-text);
  color: var(--ib-color-danger-text);
  background: var(--ib-color-danger-bg);
}

.captcha-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 15px;
}
</style>

<style>
.slider-captcha-dialog .el-dialog__body {
  padding: 10px 20px 20px;
}

.slider-captcha-dialog .el-dialog__header {
  padding: 15px 20px;
  margin-right: 0;
  border-bottom: 1px solid var(--ib-color-hairline-soft);
}
</style>
