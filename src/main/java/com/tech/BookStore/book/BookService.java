package com.tech.BookStore.book;

import com.tech.BookStore.exception.OperationNotPermittedException;
import com.tech.BookStore.history.BookTransactionHistory;
import com.tech.BookStore.user.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

import static com.tech.BookStore.book.BookSpecification.withOwnerId;

@Service
@RequiredArgsConstructor
public class BookService {
    
    private final BookMapper bookMapper;
    private final BookRepo bookRepo;
    private final BookTransactionHistoryRepo bookTransactionHistoryRepo;
    private final FileStorageService fileStorageService;

    public Integer save(BookRequest bookRequest, Authentication connectedUser)
    {
        User user = ((User) connectedUser.getPrincipal());
        Book book = bookMapper.toBook(bookRequest);
        book.setOwner(user);
        
        return bookRepo.save(book).getId();
    }
    
    public BookResponse findById(Integer bookId)
    {
        return bookRepo.findById(bookId)
                .map(bookMapper::toBookResponse)
                .orElseThrow(()->new EntityNotFoundException("No book found with the Id:: "+ bookId));
    }


    public PageResponse<BookResponse> findAllBooks(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createDate").descending());
        Page<Book> books =bookRepo.findAllDisplayableBooks(pageable,user.getId());
        List<BookResponse> bookResponseList = books.stream()
                .map(bookMapper :: toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponseList,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );

    }

    public PageResponse<BookResponse> findAllBooksByOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createDate").descending());
        Page<Book> books =bookRepo.findAll(withOwnerId(user.getId()),pageable);
        List<BookResponse> bookResponseList = books.stream()
                .map(bookMapper :: toBookResponse)
                .toList();
        return new PageResponse<>(
                bookResponseList,
                books.getNumber(),
                books.getSize(),
                books.getTotalElements(),
                books.getTotalPages(),
                books.isFirst(),
                books.isLast()
        );
    }

    public PageResponse<BorrowedBookResponse> findAllBorrowedOwner(int page, int size, Authentication connectedUser) {
        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks =bookTransactionHistoryRepo.findAllBorrowedOwner(pageable,user.getId());

        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper :: toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()



        );
    }

    public PageResponse<BorrowedBookResponse> findAllReturnedBooks(int page, int size, Authentication connectedUser) {

        User user = ((User) connectedUser.getPrincipal());
        Pageable pageable = PageRequest.of(page,size, Sort.by("createDate").descending());
        Page<BookTransactionHistory> allBorrowedBooks =bookTransactionHistoryRepo.findAllReturnedBooks(pageable,user.getId());

        List<BorrowedBookResponse> bookResponses = allBorrowedBooks.stream()
                .map(bookMapper :: toBorrowedBookResponse)
                .toList();

        return new PageResponse<>(
                bookResponses,
                allBorrowedBooks.getNumber(),
                allBorrowedBooks.getSize(),
                allBorrowedBooks.getTotalElements(),
                allBorrowedBooks.getTotalPages(),
                allBorrowedBooks.isFirst(),
                allBorrowedBooks.isLast()

        );

    }

    public Integer updateShareableStatus(Integer bookId, Authentication connectedUser) {
        Book book =  bookRepo.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("No book found with the Id::"+bookId));
        User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cant update books shareable status");

        }
        book.setShareable(!book.isShareable());
        bookRepo.save(book);
        return bookId;
    }

    public Integer updateArchivedStatus(Integer bookId, Authentication connectedUser) {
        Book book =  bookRepo.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("No book found with the Id::"+bookId));
        User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cant update others books archived status");

        }
        book.setShareable(!book.isShareable());
        bookRepo.save(book);
        return bookId;
    }

    public Integer borrowBooks(Integer bookId, Authentication connectedUser) {
        Book book =  bookRepo.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("No book found with the Id::"+bookId));
        if(book.isArchived() || book.isShareable())
        {
            throw new OperationNotPermittedException("The requested bok cant be borrowed");

        }
        User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cant borrow your own book");

        }

        final boolean isAlreadyBorrowed = bookTransactionHistoryRepo.isAlreadyBorrowed(bookId,user.getId());

        if(isAlreadyBorrowed)
        {
            throw new OperationNotPermittedException("The requested book is already borrowed");
        }

        BookTransactionHistory bookTransactionHistory =  BookTransactionHistory.builder()
                .user(user)
                .book(book)
                .returned(false)
                .returnApproved(false)
                .build();
        return bookTransactionHistoryRepo.save(bookTransactionHistory).getId();
    }

    public Integer returnBorrowBook(Integer bookId, Authentication connectedUser) {
        Book book =  bookRepo.findById(bookId)
                .orElseThrow(()->new EntityNotFoundException("No book found with the Id::"+bookId));
        if(book.isArchived() || book.isShareable())
        {
            throw new OperationNotPermittedException("The requested bok cant be borrowed since its archived or not shareable");

        }
        User user = ((User) connectedUser.getPrincipal());
        if(!Objects.equals(book.getOwner().getId(),user.getId())){
            throw new OperationNotPermittedException("You cant borrow your own book");

        }

        BookTransactionHistory transactionHistory = bookTransactionHistoryRepo.findByBookIdAndUserId(bookId,user.getId())
                .orElseThrow(()-> new OperationNotPermittedException("You did not borrow this book"));

        return bookTransactionHistoryRepo.save(transactionHistory).getId();

    }

    public Integer approveReturnBorrowedBook(Integer bookId, Authentication connectedUser) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("No book found with the ID::"+bookId));
        if(book.isArchived() || !book.isShareable())
        {
            throw new OperationNotPermittedException("The requested book cannot be borrowed since its archived or not shareable");
        }
        User user = ((User) connectedUser.getPrincipal());
        if(Objects.equals(book.getOwner().getId(),user.getId()))
        {
            throw new OperationNotPermittedException("You cannot borrow or return your own book");
        }
        BookTransactionHistory bookTransactionHistory =  bookTransactionHistoryRepo.findByBookIdAndOwnerId(bookId, user.getId())
                .orElseThrow(()->new OperationNotPermittedException("The book is not returned yet.You cannot approve"));
        bookTransactionHistory.setReturnApproved(true);
        return bookTransactionHistoryRepo.save(bookTransactionHistory).getId();

    }

    public void uploadBookCoverPicture(MultipartFile file, Authentication connectedUser, Integer bookId) {
        Book book = bookRepo.findById(bookId)
                .orElseThrow(()-> new EntityNotFoundException("No book found with the Id::" + bookId));
        User user = ((User)  connectedUser.getPrincipal());
        var bookCover = fileStorageService.saveFile(file,book,user.getId());
        book.setBookCover(bookCover);
        bookRepo.save(book);
    }
}
