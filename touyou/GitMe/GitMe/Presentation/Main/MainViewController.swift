//
//  MainViewController.swift
//  GitMe
//
//  Created by 藤井陽介 on 2018/01/22.
//  Copyright © 2018 touyou. All rights reserved.
//

import UIKit
import RxSwift

// MARK: - MainViewController

class MainViewController: UIViewController {

    // MARK: Internal

    var presenter: (MainPresenterProtocol & UITableViewDataSource)!

    // MARK: Life Cycle

    override func awakeFromNib() {

        super.awakeFromNib()

        MainContainer.shared.configure(self)
    }

    override func viewDidLoad() {

        super.viewDidLoad()

        refreshControl.addTarget(self, action: #selector(self.refresh(_:)), for: .valueChanged)
        self.navigationController?.setupBarColor()
    }

    override func viewDidAppear(_ animated: Bool) {

        super.viewDidAppear(animated)

        if !presenter.isLoggedIn {

            TabBarController.router.openLogInView()
        } else {

            presenter.fetchUser()
            presenter.logInData
                .asObservable()
                .observeOn(MainScheduler.instance)
                .subscribe { [unowned self] event in

                    switch event {
                    case .next(let value):
                        TabBarController.router.openLoadingWindow(userInfo: value)
                        self.presenter.reload { [weak self] in

                            guard let `self` = self else { return }

                            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.3, execute: {

                                TabBarController.router.closeLoadingWindow()
                            })
                            self.tableView.reloadData()
                        }
                    case .error(let error):
                        print(error)
                    case .completed:
                        break
                    }
                }.disposed(by: disposeBag)
        }
    }

    // MARK: Private

    private let disposeBag = DisposeBag()
    private let refreshControl = UIRefreshControl()

    private var isLoading: Bool = false

    @IBOutlet private weak var tableView: UITableView! {

        didSet {

            tableView.register(EventCardTableViewCell.self)
            tableView.dataSource = presenter
            tableView.delegate = self
            tableView.refreshControl = refreshControl
            tableView.rowHeight = UITableViewAutomaticDimension
            tableView.estimatedRowHeight = 155.0
            tableView.allowsSelection = false
            tableView.allowsMultipleSelection = false
        }
    }

    @objc private func refresh(_ sender: UIRefreshControl) {

        presenter.reload { [weak self] in

            guard let `self` = self else { return }

            sender.endRefreshing()
            self.tableView.reloadData()
        }
    }
}

// MARK: - TableView Delegate

extension MainViewController: UITableViewDelegate {

    func scrollViewDidScroll(_ scrollView: UIScrollView) {

        if tableView.contentOffset.y + tableView.frame.size.height > tableView.contentSize.height && tableView.isDragging && !self.isLoading {

            self.isLoading = true
            presenter.loadMore { [weak self] in

                guard let `self` = self else { return }

                self.isLoading = false
                self.tableView.reloadData()
            }
        }
    }

}

// MARK: - Storyboard Instantiable

extension MainViewController: StoryboardInstantiable {}
