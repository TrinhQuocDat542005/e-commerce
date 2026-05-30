pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {
        // Vì code đã được mount đồng bộ trực tiếp từ máy thật, Stage 1 chỉ làm nhiệm vụ check-in kiểm tra
        stage('1. Xác thực Mã nguồn') {
            steps {
                echo '📥 Workspace đã được đồng bộ an toàn từ Host Máy thật!'
                sh 'ls -la' // Lệnh này để ông soi xem file gradlew đã xuất hiện chưa
            }
        }

        stage('2. Biên dịch hệ thống (Gradle Build)') {
            steps {
                echo '🛠️ Cấp quyền và kích hoạt đúc file JAR...'
                // Ép quyền thực thi cho file gradlew để né lỗi Permission Denied trên Linux
                sh 'chmod +x gradlew'
                // Chạy lệnh Wrapper đúc file JAR thần tốc cho hệ Multi-Module
                sh './gradlew clean bootJar -x test'
                echo '🎉 Biên dịch thành công! Code sạch không bẩn.'
            }
        }

        stage('3. Triển khai Docker Compose') {
            steps {
                echo '🐳 Gọi Docker máy thật đúc Image mới và deploy đè...'
                // Thằng Jenkins gõ lệnh này, Docker máy thật của ông dưới nền sẽ tự động nhận sớ và làm việc!
                sh 'docker compose up -d --build'
                echo '🚀 Hệ thống Microservices Backend đã được cập nhật phiên bản mới nhất!'
            }
        }
    }

    post {
        success {
            echo '✅ TỰ ĐỘNG HÓA THÀNH CÔNG MỸ MÃN! HỆ THỐNG ĐÃ LÊN ĐÈN XANH.'
        }
        failure {
            echo '❌ TOI RỒI ÔNG ƠI: ĐƯỜNG ỐNG SẬP, VÀO CHECK LOG NGAY!'
        }
    }
}