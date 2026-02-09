$(document).ready(function() {
    var userRoles = $('#user-roles').data('roles');
    var isAdmin = userRoles ? userRoles.toString().includes('ADMIN') : false;

    $.ajax({
        url: '/api/v1/books',
        type: 'GET',
        dataType: 'json',
        success: function(data) {
            let cardsHTML = '';
            $.each(data, function(i, item) {
                let actionButtons = '<button class="btn btn-outline-primary btn-sm" onclick="apiAddToCart(' + item.id + '); return false;">Add to Cart</button>';

                let adminActions = '';
                if (isAdmin) {
                    adminActions = '<a href="/books/edit/' + item.id + '" class="btn btn-outline-secondary btn-sm me-2">Edit</a>' +
                                   '<button class="btn btn-outline-danger btn-sm" onclick="apiDeleteBook(' + item.id + '); return false;">Delete</button>';
                }

                cardsHTML += `
                    <div class="col" id="book-${item.id}">
                        <div class="card h-100">
                            <div class="card-body d-flex flex-column">
                                <h5 class="card-title">${item.title}</h5>
                                <h6 class="card-subtitle mb-2 text-muted">${item.author}</h6>
                                <p class="card-text"><strong>Category:</strong> ${item.category || 'N/A'}</p>
                                <div class="mt-auto">
                                    <p class="card-text fs-5 text-primary-custom fw-bold">${item.price} $</p>
                                    <div class="d-flex justify-content-between align-items-center">
                                        ${actionButtons}
                                        <div class="admin-actions">
                                            ${adminActions}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
            });
            $('#book-list-container').html(cardsHTML);
        },
        error: function(xhr, status, error) {
            console.error("Error fetching books:", error);
            $('#book-list-container').html('<div class="col"><p class="text-danger">Failed to load books.</p></div>');
        }
    });
});

function apiDeleteBook(id) {
    if (confirm('Are you sure you want to delete this book?')) {
        $.ajax({
            url: '/api/v1/books/' + id,
            type: 'DELETE',
            success: function() {
                alert('Book deleted successfully!');
                $('#book-' + id).remove();
            },
            error: function(xhr, status, error) {
                alert('Error deleting book');
                console.error("Error deleting book:", error);
            }
        });
    }
}

function apiAddToCart(id) {
    $.ajax({
        url: '/api/v1/cart/add/' + id,
        type: 'POST',
        success: function() {
            alert('Book added to cart successfully!');
        },
        error: function(xhr, status, error) {
            alert('Error adding book to cart');
            console.error("Error adding book to cart:", error);
        }
    });
}