package com.fastfood.fastfood.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import com.fastfood.fastfood.entities.*;
import com.fastfood.fastfood.exceptions.InvalidDetailsForPasswordChangeException;
import com.fastfood.fastfood.exceptions.InvalidLoginCredentialsException;
import com.fastfood.fastfood.exceptions.UserExistsException;
import com.fastfood.fastfood.repository.AddressRepository;
import com.fastfood.fastfood.repository.CarryBoxRepository;
import com.fastfood.fastfood.repository.OrderRepository;
import com.fastfood.fastfood.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuthenticationDaoImpl implements AuthenticationDao {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CarryBoxRepository carryBoxRepository;
	
	@Autowired
	private AddressRepository addressRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	static int addressId=3000;
	static int carryBoxId=1000;
	Logger logger=LoggerFactory.getLogger(AuthenticationDaoImpl.class);
	@Override
	public String userLogin(String emailId, String password) throws InvalidLoginCredentialsException {
		logger.trace("Login method is accessed");
		
		try {
			User user=userRepository.findByEmailId(emailId);
			if(user==null) {
				logger.error("InvalidLoginCredentialsException thrown by the method");
				throw new InvalidLoginCredentialsException("User does not exist");
			}
			else
			{
				if(user.getEmailId().equals(emailId)&& user.getPassword().equals(password)) {
					logger.info("Login successful");
					return user.getRole();
				}
				else {
					logger.error("InvalidLoginCredentialsException thrown by the method");
					throw new InvalidLoginCredentialsException("Invalid username or password");
				}
			}


		}catch(EntityNotFoundException e) {
			logger.error("InvalidLoginCredentialsException thrown by the method");
			throw new InvalidLoginCredentialsException("Invalid username or password");
		}
	}

	@Override
	public String userSignUp(User user, Address address)throws UserExistsException {
		logger.trace("Sign Up method accessed");
		checkDatabaseBeforeSignUp(user);
		if(address!=null) {
		int currentAddressId;
		Long result=addressRepository.getCountOfAddress();
		
		int addressCount=result.intValue();
		
		if(addressCount>0) {
			currentAddressId=addressRepository.getMaxOfAddressId()+1;
			address.setAddressId(currentAddressId);
		}

		else {
			currentAddressId=addressId+1;
			address.setAddressId(currentAddressId);
		}

		System.out.println("\n\n\n ========================================================================== \n\n\n");
		System.out.println("addd :"+address);
		System.out.println("\n\n\n ========================================================================== \n\n\n");

			addressRepository.save(address);
		}
		user.setRole("Customer");
		CarryBox carryBox=new CarryBox();
		int carryBoxCount=carryBoxRepository.getCarryBoxCount().intValue();
		System.out.println("\n\n\n carryBoxCount 1 ==========================================================================>"+carryBoxCount);

		if(carryBoxCount>0) {
			System.out.println(carryBoxCount+"\n\n\n carryBoxCount >0 ==========================================================================>"+carryBoxRepository.getMaxBoxId().intValue()+1);

			carryBox.setBoxId(carryBoxRepository.getMaxBoxId().intValue()+1);
		}
		else {
			System.out.println(carryBoxCount+"\n\n\n carryBoxId==========================================================================>"+carryBoxId);
			carryBox.setBoxId(carryBoxId+5000);
		}

		System.out.println(user+"\n\n\n My CB OUt ========================================================================== >"+carryBox.toString());

		CarryBox cb = carryBoxRepository.save(carryBox);

		System.out.println(user+"\n\n\n My CB ========================================================================== >"+cb.toString());


		user.setCarryBox(cb);
		userRepository.save(user);

		System.out.println(user+"\n\n\n 1 ========================================================================== >"+address);

		if(address!=null) {
		User user1=userRepository.findByEmailId(user.getEmailId());
		Address address1=addressRepository.getById(address.getAddressId());
		user1.addAddress(address1);
		userRepository.save(user1);
		}
		return "SignUp Successful";

	}

	@Override
	public void checkDatabaseBeforeSignUp(User user) throws UserExistsException {
		logger.trace("Database Check before Sign Up accessed");
		List<String> emails=new ArrayList<>();
		List<String> phone_numbers=new ArrayList<>();
		emails=userRepository.getEmailIdList();
		phone_numbers=userRepository.getMobileNumberList();
		if(phone_numbers.contains(user.getMobileNumber())||emails.contains(user.getEmailId())) {
			logger.error("UserExistsException thrown by the method");
			throw new UserExistsException( "You already have an account!Please kindly login!!");
		}
	}

	@Override
	public String forgotPassword(String emailId, String security_question, String security_answer)
			throws InvalidDetailsForPasswordChangeException {
		logger.trace("Forgot Password method accessed");
		List<String> emails=new ArrayList<>();
		emails=userRepository.getEmailIdList();
		if(emails.contains(emailId)){
			User user=userRepository.findByEmailId(emailId);
			if(user.getSecurity_question().equals(security_question) && user.getSecurity_answer().equals(security_answer)) {
				logger.info("User verified");
				return "User";
			}
			else {
				logger.error("InvalidDetailsForPasswordChangeException thrown by the method");
				throw new InvalidDetailsForPasswordChangeException("Invalid Details!Please try again");
			}
		}
		else {
			logger.error("InvalidDetailsForPasswordChangeException thrown by the method");
			throw new InvalidDetailsForPasswordChangeException("User does not exist");
		}

	}

	@Override
	public String changePassword(String emailId,String newPassword, String reEnterNewPassword) {
		logger.trace("Change Password method accessed");
		User user=userRepository.findByEmailId(emailId);
		if(newPassword.equals(reEnterNewPassword)) {
			user.setPassword(newPassword);
			userRepository.save(user);
			return "Password successfully changed";
		}
		else
			return "Passwords do not match!Please try again";

	}

	@Override
	public List<List<Order>> getOrderList(String emailId) {
		logger.trace("Get Order List method accessed");
		List<Order> order=new ArrayList<>();
		User user=userRepository.findByEmailId(emailId);
		if(user.getRole().equals("Admin")) {
			Branch branch=user.getBranch();
			Iterator<Order> iterator=branch.getOrders().iterator();
			while(iterator.hasNext()) {
				Order order1=iterator.next();
				if(order1.getOrderStatus().equals("Placed"))
					order.add(order1);
			}
			List<Integer> orderIds=new ArrayList<>();
			Iterator<Order> iterator2=order.iterator();
			while(iterator2.hasNext()) {
				orderIds.add(iterator2.next().getOrderId());
			}
			Set<Integer> orderIds2=new HashSet<>(orderIds);
			List<List<Order>> placedOrders=new ArrayList<>();
			Iterator<Integer> iterator3=orderIds2.iterator();
			
			while(iterator3.hasNext()) {
				placedOrders.add(getOrderByOrderId(iterator3.next()));
			}
			return placedOrders;
			
		}
		return null;
	}
	
	public List<Order> getOrderByOrderId(int orderId){
			return orderRepository.getOrders(orderId);
	}

	@Override
	public List<List<Order>> getActiveOrderList(String emailId) {
		logger.trace("Get Active Order List method accessed");
		List<Order> order=new ArrayList<>();
		User user=userRepository.findByEmailId(emailId);
		if(user.getRole().equals("Admin")) {
			Branch branch=user.getBranch();
			Iterator<Order> iterator=branch.getOrders().iterator();
			while(iterator.hasNext()) {
				Order order1=iterator.next();
				if(order1.getOrderStatus().equals("Accepted"))
					order.add(order1);
			}
			List<Integer> orderIds=new ArrayList<>();
			Iterator<Order> iterator2=order.iterator();
			while(iterator2.hasNext()) {
				orderIds.add(iterator2.next().getOrderId());
			}
			Set<Integer> orderIds2=new HashSet<>(orderIds);
			List<List<Order>> activeOrders=new ArrayList<>();
			Iterator<Integer> iterator3=orderIds2.iterator();
			
			while(iterator3.hasNext()) {
				activeOrders.add(getOrderByOrderId(iterator3.next()));
			}
			return activeOrders;
			
		}
		return null;
	}

}
