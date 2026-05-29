pipeline {
    agent any

    // Cấu hình hiển thị thời gian chạy cho đẹp và dễ tracking
    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        // ==========================================
        // STAGE 1: KÉO CODE MỚI NHẤT VỀ
        // ==========================================
        stage('1. Kéo code từ GitHub') {
            steps {
                echo '📥 Quản gia đang kéo code mới nhất từ GitHub về phân vùng sạch...'
                checkout scm
            }
        }

        // ==========================================
        // STAGE 2: BIÊN DỊCH VÀ KIỂM TRA LỖI (CI)
        // ==========================================
        stage('2. Biên dịch hệ thống (Gradle Build)') {
            steps {
                echo '🛠️ Bắt đầu compile toàn bộ hệ thống Multi-Module Spring Boot...'
                // Cấp quyền thực thi cho file gradlew trong môi trường Linux của Jenkins
                sh 'chmod +x gradlew'
                // Chạy lệnh build tất cả file JAR cùng lúc, bỏ qua chạy test để chạy cho thần tốc
                sh './gradlew clean bootJar -x test'
                echo '🎉 Biên dịch thành công! Code sạch không có lỗi cú pháp.'
            }
        }

        // ==========================================
        // STAGE 3: ĐÓNG GÓI & CẬP NHẬT CONTAINER (CD)
        // ==========================================
        stage('3. Đúc Image & Triển khai Docker') {
            steps {
                echo '🐳 Kích hoạt Docker Compose để đúc Image mới và deploy đè...'
                // Lệnh này giúp Docker Compose tự kiểm tra xem module nào có code mới thì tự đúc lại Image và khởi động lại container đó, các container khác giữ nguyên không bị gián đoạn!
                sh 'docker compose up -d --build'
                echo '🚀 Hệ thống Microservices đã được tự động cập nhật lên phiên bản mới nhất!'
            }
        }
    }

    // Báo cáo thành quả về Terminal
    post {
        success {
            echo '✅ CHÚC MỪNG ÔNG: ĐƯỜNG ỐNG CI/CD ĐÃ CHẠY THÀNH CÔNG MỸ MÃN!'
        }
        failure {
            echo '❌ TOI RỒI ÔNG ƠI: BUILD BỊ LỖI, VÀO CHECK LOG NGAY!'
        }
    }
}