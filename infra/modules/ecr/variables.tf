variable "names" {
  type = list(string)
}

variable "keep_images" {
  description = "저장소마다 남길 이미지 수 (되돌리기용)"
  type        = number
  default     = 20
}
