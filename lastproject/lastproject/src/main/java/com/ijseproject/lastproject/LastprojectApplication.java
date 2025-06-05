package com.example.lostandfound;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class LostAndFoundApplication {

	public static void main(String[] args) {
		SpringApplication.run(LostAndFoundApplication.class, args);
	}

	private Map<Long, User> users = new HashMap<>();
	private Map<Long, Item> items = new HashMap<>();
	private Map<Long, Request> requests = new HashMap<>();
	private AtomicLong userIdCounter = new AtomicLong(1);
	private AtomicLong itemIdCounter = new AtomicLong(1);
	private AtomicLong requestIdCounter = new AtomicLong(1);

	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	// ----- Enums -----
	enum ItemStatus { LOST, FOUND, CLAIMED }
	enum RequestStatus { PENDING, APPROVED, REJECTED }
	enum UserRole { ADMIN, STAFF, USER }

	// ----- Models -----
	static class User {
		Long id;
		String username;
		String password;
		UserRole role;

		public User(Long id, String username, String password, UserRole role) {
			this.id = id;
			this.username = username;
			this.password = password;
			this.role = role;
		}
	}

	static class Item {
		Long id;
		String name;
		String description;
		ItemStatus status;
		Long ownerId;

		public Item(Long id, String name, String description, ItemStatus status, Long ownerId) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.status = status;
			this.ownerId = ownerId;
		}
	}

	static class Request {
		Long id;
		Long itemId;
		Long userId;
		RequestStatus status;

		public Request(Long id, Long itemId, Long userId, RequestStatus status) {
			this.id = id;
			this.itemId = itemId;
			this.userId = userId;
			this.status = status;
		}
	}

	static class AuthRequest {
		public String username;
		public String password;
	}

	static class AuthResponse {
		public String token;

		public AuthResponse(String token) {
			this.token = token;
		}
	}

	// ----- Authentication -----
	@PostMapping("/signup")
	public ResponseEntity<?> signUp(@RequestBody AuthRequest request) {
		for (User u : users.values()) {
			if (u.username.equals(request.username)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username already exists");
			}
		}
		Long id = userIdCounter.getAndIncrement();
		User newUser = new User(id, request.username, passwordEncoder.encode(request.password), UserRole.USER);
		users.put(id, newUser);
		return ResponseEntity.ok("User registered with ID: " + id);
	}

	@PostMapping("/signin")
	public ResponseEntity<?> signIn(@RequestBody AuthRequest request) {
		for (User user : users.values()) {
			if (user.username.equals(request.username) &&
					passwordEncoder.matches(request.password, user.password)) {
				// Dummy token just for example
				return ResponseEntity.ok(new AuthResponse("dummy-jwt-token-for-" + user.username));
			}
		}
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
	}

	// ----- Item CRUD -----
	@PostMapping("/items")
	public ResponseEntity<?> createItem(@RequestBody Item item) {
		item.id = itemIdCounter.getAndIncrement();
		items.put(item.id, item);
		return ResponseEntity.ok(item);
	}

	@GetMapping("/items")
	public ResponseEntity<?> getAllItems() {
		return ResponseEntity.ok(items.values());
	}

	@PutMapping("/items/{id}")
	public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody Item updated) {
		if (!items.containsKey(id)) return ResponseEntity.notFound().build();
		Item existing = items.get(id);
		existing.name = updated.name;
		existing.description = updated.description;
		existing.status = updated.status;
		return ResponseEntity.ok(existing);
	}

	@DeleteMapping("/items/{id}")
	public ResponseEntity<?> deleteItem(@PathVariable Long id) {
		items.remove(id);
		return ResponseEntity.ok("Item deleted");
	}

	// ----- Request CRUD -----
	@PostMapping("/requests")
	public ResponseEntity<?> createRequest(@RequestBody Request request) {
		request.id = requestIdCounter.getAndIncrement();
		request.status = RequestStatus.PENDING;
		requests.put(request.id, request);
		return ResponseEntity.ok(request);
	}

	@GetMapping("/requests")
	public ResponseEntity<?> getAllRequests() {
		return ResponseEntity.ok(requests.values());
	}

	@PutMapping("/requests/{id}/status")
	public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestParam String status) {
		Request req = requests.get(id);
		if (req == null) return ResponseEntity.notFound().build();
		req.status = RequestStatus.valueOf(status.toUpperCase());
		return ResponseEntity.ok(req);
	}

	// ----- User list (for Admins) -----
	@GetMapping("/users")
	public ResponseEntity<?> getAllUsers() {
		return ResponseEntity.ok(users.values());
	}

	// ----- Log setup (simulated) -----
	@PostConstruct
	public void init() {
		System.out.println("Lost and Found App Started...");
	}

	// ----- Password encoder bean (optional for reuse) -----
	@Bean
	public PasswordEncoder encoder() {
		return passwordEncoder;
	}
}
