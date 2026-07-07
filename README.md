# Ứng dụng Quản lý công việc (Todo List) - Backend API

Dự án này là phần Backend RESTful API phục vụ cho ứng dụng Quản lý công việc (Todo List), được phát triển bằng Java Spring Boot, Hibernate/JPA và cơ sở dữ liệu MySQL.

---

## 🛠️ Công nghệ sử dụng (Tech Stack)

- **Java**: Phiên bản 21 (hoặc tương thích Java 17+)
- **Framework**: Spring Boot 3.3.1
- **Database Access**: Spring Data JPA & Hibernate
- **Database**: MySQL (môi trường chạy thực tế) & H2 (môi trường test in-memory)
- **Tiện ích**: Lombok (giảm thiểu boilerplate code)
- **Xác thực dữ liệu**: Spring Validation

---

## 🏗️ Kiến trúc ứng dụng

Dự án tuân thủ chặt chẽ **Kiến trúc 3 tầng (3-Layer Architecture)** phối hợp với mô hình **MVC (Model-View-Controller)**:
1. **Tầng Controller (`controller/` - MVC: Controller)**: Đón nhận các yêu cầu HTTP, kiểm tra tính hợp lệ của dữ liệu đầu vào (Validation) và điều phối phản hồi RESTful API.
2. **Tầng Service (`service/` - Business Layer)**: Chứa toàn bộ logic nghiệp vụ (business logic) của hệ thống.
3. **Tầng Repository (`repository/` - Data Access Layer)**: Tương tác trực tiếp với cơ sở dữ liệu qua Spring Data JPA.
4. **Model/Entity (`entity/` - MVC: Model)**: Đại diện cho cấu trúc dữ liệu của bảng `tasks` trong database.
5. **DTOs (`dto/`)**: Sử dụng Java Records để chuyển tải dữ liệu an toàn giữa các tầng mà không làm lộ thực thể Database Entity.

---

## 💾 Tự động khởi tạo dữ liệu mẫu (Auto Seed Data)

Khi ứng dụng chạy lần đầu tiên, lớp [DatabaseSeeder.java](file:///k:/Code/QuanLyCongViec/backend/src/main/java/com/todo/todolist/seeder/DatabaseSeeder.java) sẽ tự động kiểm tra xem bảng `tasks` có rỗng hay không. Nếu rỗng, nó sẽ tự động nạp sẵn **5 công việc mẫu** (với các mức độ ưu tiên, trạng thái hoàn thành và hạn chót khác nhau) vào database để bạn kiểm thử ngay lập tức mà không cần tự tạo dữ liệu.

---

## ⚙️ Cấu hình MySQL & Chạy ứng dụng

### 1. Yêu cầu hệ thống
- Có sẵn MySQL Server đang hoạt động trên máy của bạn (hoặc chạy qua Docker).

### 2. Cấu hình cơ sở dữ liệu
Cấu hình kết nối nằm trong file [application.properties](file:///k:/Code/QuanLyCongViec/backend/src/main/resources/application.properties). URL kết nối đã bật tham số `createDatabaseIfNotExist=true`, do đó **bạn không cần phải tự tạo database thủ công trong MySQL**. Hệ thống sẽ tự động tạo cơ sở dữ liệu tên `todo_db` khi khởi động.

Mặc định, ứng dụng sử dụng các thông số sau:
- Host: `localhost` (cổng `3306`)
- Tên Database: `todo_db`
- Username: `root`
- Password: (để trống)

Bạn có thể thay đổi cấu hình kết nối bằng cách **đặt biến môi trường** trước khi chạy (khuyên dùng để bảo mật):
- `DB_HOST`: Địa chỉ máy chủ MySQL (mặc định: `localhost`)
- `DB_PORT`: Cổng MySQL (mặc định: `3306`)
- `DB_NAME`: Tên database (mặc định: `todo_db`)
- `DB_USERNAME`: Tài khoản MySQL (mặc định: `root`)
- `DB_PASSWORD`: Mật khẩu MySQL (mặc định: trống)

*Hoặc bạn cũng có thể mở file [application.properties](file:///k:/Code/QuanLyCongViec/backend/src/main/resources/application.properties) và chỉnh sửa trực tiếp.*

### 3. Lệnh chạy ứng dụng
Di chuyển vào thư mục `backend/` và sử dụng Maven Wrapper đi kèm để khởi chạy:

**Trên Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Trên Linux / macOS:**
```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

Ứng dụng sẽ chạy tại cổng `8080`. API root là: `http://localhost:8080/api/tasks`

---

## 🧪 Chạy Unit Test

Hệ thống đi kèm bộ Unit Test đầy đủ cho cả tầng Service và Controller (tổng cộng 19 test cases) bao phủ các trường hợp tạo, sửa, xóa, tìm kiếm, lọc, phân trang, và validate đầu vào không hợp lệ.

Để đảm bảo khả năng build tự động ở mọi môi trường mà không cần chạy MySQL, **các Unit Test được cấu hình chạy trên H2 Database in-memory** (qua cấu hình [application.properties trong thư mục test](file:///k:/Code/QuanLyCongViec/backend/src/test/resources/application.properties)).

Chạy test bằng lệnh sau:

**Trên Windows:**
```powershell
cd backend
.\mvnw.cmd clean test
```

**Trên Linux / macOS:**
```bash
cd backend
./mvnw clean test
```

---

## 🔌 Tài liệu API Endpoints

Tất cả các API trả về dữ liệu dạng JSON.

### 1. Lấy danh sách công việc
- **Endpoint**: `GET /api/tasks`
- **Các tham số lọc/phân trang tùy chọn (Query Parameters)**:
  - `search` (String): Tìm kiếm theo từ khóa trong tiêu đề (không phân biệt chữ hoa/thường).
  - `completed` (Boolean): Lọc theo trạng thái `true` (đã hoàn thành) hoặc `false` (chưa hoàn thành).
  - `page` (Int): Số trang, bắt đầu từ `0` (mặc định: `0`).
  - `size` (Int): Số phần tử trên một trang (mặc định: `10`).
  - `sortBy` (String): Trường sắp xếp (mặc định: `createdAt`, các lựa chọn khác: `title`, `priority`, `dueDate`, `updatedAt`).
  - `sortDir` (String): Hướng sắp xếp: `asc` hoặc `desc` (mặc định: `desc`).
- **Ví dụ cURL**:
  ```bash
  curl "http://localhost:8080/api/tasks?completed=false&search=REST&sortBy=dueDate&sortDir=asc"
  ```

### 2. Xem chi tiết công việc
- **Endpoint**: `GET /api/tasks/{id}`
- **Ví dụ cURL**:
  ```bash
  curl http://localhost:8080/api/tasks/1
  ```
- **Lỗi**: Trả về `404 Not Found` nếu ID không tồn tại.

### 3. Tạo mới công việc
- **Endpoint**: `POST /api/tasks`
- **Request Body (JSON)**:
  ```json
  {
    "title": "Học viết Unit Test nâng cao",
    "description": "Tìm hiểu về MockMvc và integration tests",
    "priority": "HIGH",
    "dueDate": "2026-07-15T23:59:59"
  }
  ```
- **Lưu ý**:
  - `title` là bắt buộc, không được để trống hoặc chỉ có khoảng trắng.
  - `priority` nhận các giá trị: `LOW`, `MEDIUM`, `HIGH` (mặc định: `MEDIUM`).
  - `dueDate` định dạng ISO LocalDateTime (tùy chọn).
- **Ví dụ cURL**:
  ```bash
  curl -X POST http://localhost:8080/api/tasks \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"Học viết Unit Test\",\"priority\":\"HIGH\"}"
  ```
- **Validation Error**: Nếu gửi `title` trống, API trả về `400 Bad Request` dạng:
  ```json
  {
    "timestamp": "2026-07-07T21:00:00.000",
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed",
    "details": {
      "title": "Title must not be blank"
    }
  }
  ```

### 4. Cập nhật toàn bộ công việc
- **Endpoint**: `PUT /api/tasks/{id}`
- **Request Body (JSON)**: Cần truyền đầy đủ thông tin:
  ```json
  {
    "title": "Học viết Unit Test (Đã cập nhật)",
    "description": "Mô tả mới",
    "completed": true,
    "priority": "MEDIUM",
    "dueDate": "2026-07-20T18:00:00"
  }
  ```
- **Ví dụ cURL**:
  ```bash
  curl -X PUT http://localhost:8080/api/tasks/1 \
    -H "Content-Type: application/json" \
    -d "{\"title\":\"Cập nhật tiêu đề\",\"completed\":true,\"priority\":\"MEDIUM\"}"
  ```

### 5. Đảo trạng thái hoàn thành (Toggle)
- **Endpoint**: `PATCH /api/tasks/{id}/toggle`
- **Mô tả**: Tự động chuyển đổi `completed` từ `true` thành `false` hoặc ngược lại. Không cần truyền body.
- **Ví dụ cURL**:
  ```bash
  curl -X PATCH http://localhost:8080/api/tasks/1/toggle
  ```

### 6. Xóa công việc
- **Endpoint**: `DELETE /api/tasks/{id}`
- **Ví dụ cURL**:
  ```bash
  curl -X DELETE http://localhost:8080/api/tasks/1
  ```
- **Phản hồi**: Trả về `204 No Content` khi xóa thành công.

---

## 💻 Giao diện người dùng (Frontend)

Giao diện người dùng được xây dựng bằng công nghệ thuần **HTML5, Vanilla CSS và JavaScript (ES6)** áp dụng các quy chuẩn thiết kế từ [ui-ux-pro-max-skill](file:///k:/Code/QuanLyCongViec/ui-ux-pro-max-skill) theo phong cách **Glassmorphism (Kính mờ)** và **Modern Dark Mode** bóng bẩy.

### 🌟 Tính năng nổi bật của Giao diện:
- **Hiệu ứng Kính mờ cao cấp**: Kết hợp làm mờ hậu cảnh (`backdrop-filter`) và các vùng hạt sáng chuyển động mượt mà (Aurora blobs).
- **Thanh đo tiến trình**: Cập nhật phần trăm hoàn thành công việc theo thời gian thực.
- **Tìm kiếm thông minh**: Ô nhập liệu tìm kiếm tích hợp bộ trễ Debounce tránh spam yêu cầu lên máy chủ.
- **Bento Grid Layout**: Bố cục lưới bento trực quan, co dãn hiển thị tốt trên mọi kích thước màn hình (Mobile, Tablet, Desktop).
- **Bắt lỗi thông minh**: Bắt lỗi trống tiêu đề ngay tại client và ánh xạ chi tiết lỗi validation trả về từ Backend trực tiếp lên form.
- **Thông báo Toast**: Hộp thông báo nổi phản hồi tức thì các hành động Thêm/Sửa/Xóa kèm icon đẹp mắt.

### 🚀 Hướng dẫn chạy Giao diện Frontend:
1. Đảm bảo Backend Spring Boot đang chạy tại `http://localhost:8080`.
2. Do Backend đã cấu hình CORS toàn cục cho phép mọi Origin, bạn chỉ cần mở file [index.html](file:///k:/Code/QuanLyCongViec/frontend/index.html) bằng cách click đúp trực tiếp hoặc mở bằng trình duyệt Web bất kỳ (Chrome, Edge, Firefox, Safari) để trải nghiệm ứng dụng.
3. *Khuyên dùng*: Bạn có thể dùng các extension như **Live Server** (trên VS Code) hoặc các công cụ phục vụ HTTP tĩnh để chạy Frontend dưới dạng một máy chủ local nhằm đem lại trải nghiệm mượt mà nhất.

